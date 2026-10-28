package com.tlnt.server.net.transport;

/**
 * Sự kiện từ tầng mạng. CHẠY TRÊN LUỒNG I/O: phải xử lý thật nhanh, việc nặng thì đẩy sang luồng khác
 * (xem {@code PacketDispatcher}).
 */
public interface NetListener {

   void connected(NetConnection connection);

   /** 1 DataPacket từ client. {@code data} thuộc về người nhận (không bị tái sử dụng). */
   void received(NetConnection connection, byte opcode, byte[] data);

   /** Gọi đúng 1 lần cho mỗi kết nối đã {@link #connected}. */
   void disconnected(NetConnection connection);
}
