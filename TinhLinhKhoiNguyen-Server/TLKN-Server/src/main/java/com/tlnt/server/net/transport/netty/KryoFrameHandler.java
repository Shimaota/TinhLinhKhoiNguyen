package com.tlnt.server.net.transport.netty;

import com.tlnt.server.net.transport.NetListener;
import com.tlnt.server.net.wire.Wire;
import com.tlnt.server.util.Log;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Giải mã frame KryoNet phía nhận + vòng đời kết nối (xem {@link Wire} cho định dạng). 1 instance / kết nối.
 *
 * <ul>
 *   <li>Kết nối: gửi RegisterTCP ngay (client KryoNet chờ gói này trong 1 giây, không có là coi như lỗi).</li>
 *   <li>DataPacket -> {@link NetListener#received}. KeepAlive -> bỏ qua. Ping -> trả lời isReply=true.</li>
 *   <li>8 giây chưa ghi gì -> gửi KeepAlive (client tự ngắt nếu 12 giây không nhận được gì): kiểm tra mỗi
 *       {@link #KEEP_ALIVE_CHECK_SECONDS} giây theo {@link NettyConnection#lastWriteNanos} — không dùng writer-idle của
 *       {@code IdleStateHandler} vì nó tạo 1 promise + listener cho MỖI lần ghi (đo được ~7% CPU lúc đông người).
 *       12 giây không nhận gì (client gửi KeepAlive mỗi 8 giây) -> ngắt ({@code IdleStateHandler} chỉ đo phía đọc).</li>
 * </ul>
 */
final class KryoFrameHandler extends ByteToMessageDecoder {

   private final NettyServer server;
   private final NetListener listener;
   private final int maxFrameBytes;
   private NettyConnection connection;
   private ScheduledFuture<?> keepAlive;

   /** Chưa ghi gì bấy nhiêu giây thì gửi KeepAlive; kiểm tra mỗi {@link #KEEP_ALIVE_CHECK_SECONDS} giây (trễ tối đa 10 s). */
   private static final long KEEP_ALIVE_NANOS = TimeUnit.SECONDS.toNanos(8);
   private static final int KEEP_ALIVE_CHECK_SECONDS = 2;

   KryoFrameHandler(NettyServer server, NetListener listener, int maxFrameBytes) {
      this.server = server;
      this.listener = listener;
      this.maxFrameBytes = maxFrameBytes;
   }

   @Override
   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      connection = server.register(ctx.channel());
      connection.sendFrame(Wire.encodeRegisterTcp(connection.id()));
      keepAlive = ctx.executor().scheduleAtFixedRate(this::sendKeepAliveIfIdle, KEEP_ALIVE_CHECK_SECONDS,
            KEEP_ALIVE_CHECK_SECONDS, TimeUnit.SECONDS);
      listener.connected(connection);
      super.channelActive(ctx);
   }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (keepAlive != null) {
         keepAlive.cancel(false);
      }
      if (connection != null) {
         server.unregister(connection);
         listener.disconnected(connection);
      }
      super.channelInactive(ctx);
   }

   @Override
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
      while (in.readableBytes() >= Wire.LENGTH_BYTES) {
         int length = in.getInt(in.readerIndex());
         if (length <= 0 || length > maxFrameBytes) {
            Log.warn("Ket noi %s gui frame do dai %d khong hop le, ngat", connection, length);
            in.skipBytes(in.readableBytes());
            ctx.close();
            return;
         }
         if (in.readableBytes() < Wire.LENGTH_BYTES + length) {
            return;
         }
         in.skipBytes(Wire.LENGTH_BYTES);
         int end = in.readerIndex() + length;
         try {
            readObject(ctx, in, end);
         } catch (RuntimeException e) {
            Log.warn("Ket noi %s gui frame loi (%s), ngat", connection, e.getMessage());
            in.skipBytes(in.readableBytes());
            ctx.close();
            return;
         }
         in.readerIndex(end);
      }
   }

   private void readObject(ChannelHandlerContext ctx, ByteBuf in, int end) {
      int classId = readVarInt(in, end);
      switch (classId) {
         case Wire.ID_DATA_PACKET -> {
            byte cmd = readByte(in, end);
            int lengthPlusOne = readVarInt(in, end);
            byte[] data = null;
            if (lengthPlusOne > 0) {
               int length = lengthPlusOne - 1;
               if (length > end - in.readerIndex()) {
                  throw new IllegalStateException("DataPacket dai hon frame");
               }
               data = new byte[length];
               in.readBytes(data);
            }
            listener.received(connection, cmd, data);
         }
         case Wire.ID_KEEP_ALIVE -> {
            // chỉ để giữ kết nối (IdleStateHandler đã ghi nhận có dữ liệu tới)
         }
         case Wire.ID_PING -> {
            int id = Wire.unZigZag(readVarInt(in, end));
            boolean isReply = readByte(in, end) != 0;
            if (!isReply) {
               connection.sendFrame(Wire.encodePing(id, true));
            }
         }
         default -> throw new IllegalStateException("class id " + classId + " khong ho tro");
      }
   }

   private static byte readByte(ByteBuf in, int end) {
      if (in.readerIndex() >= end) {
         throw new IllegalStateException("het frame");
      }
      return in.readByte();
   }

   private static int readVarInt(ByteBuf in, int end) {
      int result = 0;
      for (int shift = 0; shift < 35; shift += 7) {
         int b = readByte(in, end);
         result |= (b & 0x7F) << shift;
         if ((b & 0x80) == 0) {
            return result;
         }
      }
      throw new IllegalStateException("varint qua dai");
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
      if (event instanceof IdleStateEvent idle) {
         if (idle.state() == IdleState.READER_IDLE) {
            Log.debug("Ket noi %s qua 12 giay khong gui gi, ngat", connection);
            ctx.close();
         }
         return;
      }
      super.userEventTriggered(ctx, event);
   }

   /** Chạy trên event loop của kết nối (cùng luồng với {@link NettyConnection#drain}). */
   private void sendKeepAliveIfIdle() {
      if (connection != null && System.nanoTime() - connection.lastWriteNanos >= KEEP_ALIVE_NANOS) {
         connection.sendFrame(Wire.KEEP_ALIVE_FRAME);
      }
   }

   @Override
   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      if (cause instanceof IOException) {
         Log.debug("Ket noi %s loi I/O: %s", connection, cause.getMessage()); // client tắt ngang, mất mạng...
      } else {
         Log.error("Loi ket noi " + connection, cause);
      }
      ctx.close();
   }
}
