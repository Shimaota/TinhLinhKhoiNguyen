package com.tlnt.server.net.transport.netty;

import com.tlnt.server.net.transport.NetListener;
import com.tlnt.server.net.transport.NetServer;
import com.tlnt.server.util.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tầng mạng Netty, nói đúng giao thức KryoNet của client (xem {@link KryoFrameHandler}).
 *
 * So với KryoNet: nhiều luồng I/O ({@code net.ioThreads}, mặc định = số nhân), không quét toàn bộ kết nối mỗi lần có
 * gói tới, bộ đệm cấp phát theo nhu cầu thay vì 128 KB cố định / kết nối, và ghi gộp nhiều gói / 1 lần flush.
 * Linux dùng epoll nếu có, Windows/macOS dùng NIO.
 */
public final class NettyServer implements NetServer {

   /** Giống KryoNet: client gửi KeepAlive mỗi 8 giây; 12 giây im lặng coi như chết (KeepAlive phía server: KryoFrameHandler). */
   private static final int READ_TIMEOUT_SECONDS = 12;

   private final NetListener listener;
   private final int ioThreads;
   private final int maxFrameBytes;
   private final int maxPendingWriteBytes;
   /** Cổng WebSocket cho client web, 0 = tắt (xem {@link WebSocketBridge}). */
   private final int wsPort;
   private final AtomicInteger nextId = new AtomicInteger(1);
   private final AtomicInteger connections = new AtomicInteger();

   private EventLoopGroup bossGroup;
   private EventLoopGroup workerGroup;
   private Channel serverChannel;
   private Channel wsChannel;

   public NettyServer(NetListener listener, int ioThreads, int maxFrameBytes, int maxPendingWriteBytes, int wsPort) {
      this.listener = listener;
      this.ioThreads = ioThreads;
      this.maxFrameBytes = maxFrameBytes;
      this.maxPendingWriteBytes = maxPendingWriteBytes;
      this.wsPort = wsPort;
   }

   @Override
   public void start(int port) throws InterruptedException {
      boolean epoll = Epoll.isAvailable();
      Class<? extends ServerChannel> channelClass;
      if (epoll) {
         bossGroup = new EpollEventLoopGroup(1, new DefaultThreadFactory("net-accept"));
         workerGroup = new EpollEventLoopGroup(ioThreads, new DefaultThreadFactory("net-io"));
         channelClass = EpollServerSocketChannel.class;
      } else {
         bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("net-accept"));
         workerGroup = new NioEventLoopGroup(ioThreads, new DefaultThreadFactory("net-io"));
         channelClass = NioServerSocketChannel.class;
      }

      ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(channelClass)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true) // giống KryoNet: gói nhỏ gửi ngay
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                  new WriteBufferWaterMark(Math.min(32 * 1024, maxPendingWriteBytes / 2), maxPendingWriteBytes))
            .childHandler(new ChannelInitializer<SocketChannel>() {
               @Override
               protected void initChannel(SocketChannel ch) {
                  ch.pipeline()
                        .addLast("idle", new IdleStateHandler(READ_TIMEOUT_SECONDS, 0, 0, TimeUnit.SECONDS))
                        .addLast("kryo", new KryoFrameHandler(NettyServer.this, listener, maxFrameBytes));
               }
            });
      serverChannel = bootstrap.bind(port).sync().channel();
      Log.info("Tang mang Netty (%s, %d luong I/O) dang nghe cong %d", epoll ? "epoll" : "nio", ioThreads, port);

      if (wsPort > 0) {
         // Cùng event loop + tuỳ chọn socket; khác pipeline: HTTP upgrade -> WebSocket -> cùng KryoFrameHandler.
         ServerBootstrap ws = bootstrap.clone().childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
               ch.pipeline()
                     .addLast("http", new HttpServerCodec())
                     .addLast("http-agg", new HttpObjectAggregator(8 * 1024))
                     .addLast("ws", new WebSocketServerProtocolHandler(WebSocketServerProtocolConfig.newBuilder()
                           .websocketPath("/ws")
                           .maxFramePayloadLength(maxFrameBytes)
                           .handshakeTimeoutMillis(READ_TIMEOUT_SECONDS * 1000L)
                           .build()))
                     .addLast("ws-agg", new WebSocketFrameAggregator(maxFrameBytes))
                     .addLast("ws-bridge", new WebSocketBridge(NettyServer.this, listener, maxFrameBytes,
                           READ_TIMEOUT_SECONDS));
            }
         });
         wsChannel = ws.bind(wsPort).sync().channel();
         Log.info("WebSocket cho client web dang nghe cong %d (ws://<host>:%d/ws)", wsPort, wsPort);
      }
   }

   NettyConnection register(Channel channel) {
      int id = nextId.getAndIncrement();
      if (id <= 0) { // quay vòng sau 2 tỉ kết nối
         nextId.set(2);
         id = 1;
      }
      connections.incrementAndGet();
      return new NettyConnection(id, channel);
   }

   void unregister(NettyConnection connection) {
      connections.decrementAndGet();
   }

   @Override
   public void stop() {
      if (serverChannel != null) {
         serverChannel.close().syncUninterruptibly();
      }
      if (wsChannel != null) {
         wsChannel.close().syncUninterruptibly();
      }
      if (workerGroup != null) {
         // Đóng từng kết nối -> channelInactive -> listener.disconnected (lưu nhân vật).
         workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
      }
      if (bossGroup != null) {
         bossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
      }
   }

   @Override
   public int connectionCount() {
      return connections.get();
   }

   @Override
   public String name() {
      return "netty";
   }
}
