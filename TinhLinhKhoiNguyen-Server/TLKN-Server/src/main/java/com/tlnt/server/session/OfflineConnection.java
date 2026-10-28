package com.tlnt.server.session;

import com.tlnt.server.net.Packet;
import com.tlnt.server.net.transport.NetConnection;

/**
 * "Kết nối" của nhân vật đang treo offline sau khi client đã ngắt: giữ id + IP cũ (log, đếm), mọi lần gửi bị bỏ ngay
 * (không mã hoá, không xếp hàng) -> bot treo offline chỉ tốn CPU cho logic game, không tốn cho mạng.
 */
final class OfflineConnection implements NetConnection {

   private final int id;
   private final String remoteIp;
   private volatile Object attachment;

   OfflineConnection(int id, String remoteIp) {
      this.id = id;
      this.remoteIp = remoteIp;
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
   }

   @Override
   public void sendFrames(byte[] frames, int offset, int length) {
   }

   @Override
   public void close() {
   }

   @Override
   public Object attachment() {
      return attachment;
   }

   @Override
   public void attach(Object attachment) {
      this.attachment = attachment;
   }
}
