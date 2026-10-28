package com.tlnt.server.net.transport;

/** Tầng mạng chọn bằng {@code net.transport} trong server.properties: {@code netty} (mặc định) hoặc {@code kryonet}. */
public interface NetServer {

   void start(int port) throws Exception;

   /** Ngừng nhận kết nối mới và đóng mọi kết nối (mỗi kết nối -> {@link NetListener#disconnected}). */
   void stop();

   int connectionCount();

   String name();
}
