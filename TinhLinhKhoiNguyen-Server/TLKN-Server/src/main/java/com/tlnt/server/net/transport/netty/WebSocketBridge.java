package com.tlnt.server.net.transport.netty;

import com.tlnt.server.net.transport.NetListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cầu nối cho client web (trình duyệt không mở được TCP): message WebSocket nhị phân chở NGUYÊN luồng byte KryoNet
 * (frame int32 độ dài | object, xem {@link com.tlnt.server.net.wire.Wire}), nên phía sau dùng lại {@link KryoFrameHandler}.
 *
 * <ul>
 *   <li>Vào: BinaryWebSocketFrame -> ByteBuf (KryoFrameHandler tự ghép frame vắt qua nhiều message).</li>
 *   <li>Ra: ByteBuf của {@link NettyConnection} -> BinaryWebSocketFrame.</li>
 *   <li>KryoFrameHandler gửi RegisterTCP ngay trong channelActive: chỉ gắn nó SAU khi bắt tay WebSocket xong, rồi
 *       phát lại channelActive cho phần pipeline phía sau.</li>
 * </ul>
 */
final class WebSocketBridge extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {

   private final NettyServer server;
   private final NetListener listener;
   private final int maxFrameBytes;
   private final int readTimeoutSeconds;

   WebSocketBridge(NettyServer server, NetListener listener, int maxFrameBytes, int readTimeoutSeconds) {
      this.server = server;
      this.listener = listener;
      this.maxFrameBytes = maxFrameBytes;
      this.readTimeoutSeconds = readTimeoutSeconds;
   }

   @Override
   public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
      if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
         ctx.pipeline()
               .addLast("idle", new IdleStateHandler(readTimeoutSeconds, 0, 0, TimeUnit.SECONDS))
               .addLast("kryo", new KryoFrameHandler(server, listener, maxFrameBytes));
         ctx.fireChannelActive();
         return;
      }
      super.userEventTriggered(ctx, event);
   }

   @Override
   protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
      out.add(new BinaryWebSocketFrame(msg.retain()));
   }

   @Override
   protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) {
      if (frame instanceof BinaryWebSocketFrame) {
         out.add(frame.content().retain());
      }
      // Text frame: client game không gửi, bỏ qua. Ping / Close: WebSocketServerProtocolHandler đã xử lý.
   }
}
