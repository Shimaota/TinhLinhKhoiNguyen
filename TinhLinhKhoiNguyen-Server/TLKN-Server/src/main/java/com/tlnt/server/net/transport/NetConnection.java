package com.tlnt.server.net.transport;

import com.tlnt.server.net.Packet;

/**
 * 1 kết nối TCP tới client, không phụ thuộc thư viện mạng (Netty hay KryoNet). Mọi method gọi được từ bất kỳ
 * luồng nào.
 */
public interface NetConnection {

   int id();

   String remoteIp();

   /** Xếp gói vào hàng gửi, không chặn luồng gọi. Kết nối đã đóng thì bỏ qua. */
   void send(Packet packet);

   /**
    * Gửi 1 đoạn gồm NHIỀU frame hoàn chỉnh nối liền ({@code frames[offset..offset+length)}). Mảng không được sửa sau
    * khi gọi (tầng mạng giữ tham chiếu tới lúc ghi xong). Dùng để phát gộp: vd mọi gói di chuyển trong 1 tick khu.
    */
   void sendFrames(byte[] frames, int offset, int length);

   /** Đóng kết nối (sau khi gửi nốt những gì đã xếp hàng). {@link NetListener#disconnected} sẽ được gọi. */
   void close();

   /** Đối tượng gắn kèm (PlayerSession) — tra O(1), không cần map connectionId -> session. */
   Object attachment();

   void attach(Object attachment);
}
