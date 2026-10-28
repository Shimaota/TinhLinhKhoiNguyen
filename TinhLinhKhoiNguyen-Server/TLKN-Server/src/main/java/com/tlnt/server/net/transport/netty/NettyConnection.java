package com.tlnt.server.net.transport.netty;

import com.tlnt.server.net.Packet;
import com.tlnt.server.net.transport.NetConnection;
import com.tlnt.server.util.Log;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.internal.PlatformDependent;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kết nối Netty. Gửi gói KHÔNG gọi {@code writeAndFlush} từng gói (mỗi lần là 1 task + 1 syscall): gói xếp vào
 * hàng đợi MPSC, rồi 1 task trên event loop ghi hết và flush 1 lần. Vd 1 tick khu phát 29 gói di chuyển cho 1 người
 * -> 1 task, 1 syscall.
 *
 * Client chậm/treo: bộ đệm ghi vượt mốc cao ({@code net.maxPendingWriteBytes}) -> ngắt, thay vì giữ RAM vô hạn
 * (KryoNet cũ thì quay vòng chờ, chiếm luồng).
 */
final class NettyConnection implements NetConnection {

   private final int id;
   private final Channel channel;
   private final String remoteIp;
   private final Queue<ByteBuf> outbound = PlatformDependent.newMpscQueue();
   private final AtomicBoolean drainScheduled = new AtomicBoolean();
   private final Runnable drainTask = this::drain;
   private volatile Object attachment;
   /** System.nanoTime() lần ghi gần nhất — chỉ luồng event loop đọc/ghi ({@link #drain}, KeepAlive). */
   long lastWriteNanos = System.nanoTime();

   NettyConnection(int id, Channel channel) {
      this.id = id;
      this.channel = channel;
      InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
      this.remoteIp = address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : "?";
   }

   @Override
   public int id() {
      return id;
   }

   @Override
   public String remoteIp() {
      return remoteIp;
   }

   @Override
   public void send(Packet packet) {
      sendFrame(packet.frame());
   }

   @Override
   public void sendFrames(byte[] frames, int offset, int length) {
      if (length > 0) {
         enqueue(Unpooled.wrappedBuffer(frames, offset, length));
      }
   }

   void sendFrame(byte[] frame) {
      enqueue(Unpooled.wrappedBuffer(frame));
   }

   private void enqueue(ByteBuf buffer) {
      if (!channel.isActive()) {
         return;
      }
      outbound.offer(buffer);
      if (drainScheduled.compareAndSet(false, true)) {
         channel.eventLoop().execute(drainTask);
      }
   }

   private void drain() {
      drainScheduled.set(false);
      boolean wrote = false;
      ByteBuf buffer;
      while ((buffer = outbound.poll()) != null) {
         if (channel.isActive()) {
            channel.write(buffer, channel.voidPromise());
            wrote = true;
         } else {
            buffer.release();
         }
      }
      if (!wrote) {
         return;
      }
      lastWriteNanos = System.nanoTime();
      channel.flush();
      if (!channel.isWritable()) {
         Log.warn("Ket noi #%d (%s) nhan goi qua cham (dong > %d byte chua gui), ngat ket noi", id, remoteIp,
               channel.config().getWriteBufferHighWaterMark());
         channel.close();
      }
   }

   @Override
   public void close() {
      channel.close();
   }

   @Override
   public Object attachment() {
      return attachment;
   }

   @Override
   public void attach(Object attachment) {
      this.attachment = attachment;
   }

   @Override
   public String toString() {
      return "#" + id + " " + remoteIp;
   }
}
