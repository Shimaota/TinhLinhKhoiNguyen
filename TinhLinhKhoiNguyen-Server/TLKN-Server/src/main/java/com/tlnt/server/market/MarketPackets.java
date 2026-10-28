package com.tlnt.server.market;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;

import java.util.List;

/** Gói S→C op 36 (client {@code ServerPacketHandler.handleTradeUpdate} — tên sai, thật ra là kí gửi). */
final class MarketPackets {

   private MarketPackets() {
   }

   /** Sub 0: xoá danh sách "Cửa hàng" + "Đang bán" (client không tự xoá khi nhận sub 1). */
   static Message clear() {
      Message msg = new Message(Opcode.MARKET);
      msg.writeByte(0);
      return msg;
   }

   /**
    * Sub 1: thêm vào danh sách: int n, n×(int id, int người bán, str tên người bán, int giá, item, long ms còn lại).
    * Client tự tách món có người bán = mình sang tab "Đang bán".
    */
   static Message list(List<Listing> listings, long now) {
      Message msg = new Message(Opcode.MARKET);
      msg.writeByte(1);
      msg.writeInt(listings.size());
      for (Listing l : listings) {
         msg.writeInt(l.id());
         msg.writeInt(l.sellerId());
         msg.writeString(l.sellerName());
         msg.writeInt(l.price());
         ItemPackets.writeItem(msg, l.item());
         msg.writeLong(Math.max(0, l.expireAt() - now));
      }
      return msg;
   }

   /** Sub 2: bỏ món đang chọn ở ô "Vật phẩm bán" (sau khi đăng bán xong). */
   static Message clearSellItem() {
      Message msg = new Message(Opcode.MARKET);
      msg.writeByte(2);
      return msg;
   }
}
