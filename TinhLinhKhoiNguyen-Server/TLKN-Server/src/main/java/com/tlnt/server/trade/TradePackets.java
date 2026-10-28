package com.tlnt.server.trade;

import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.net.Message;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;

/** Gói S→C opcode 56 ({@code ServerPacketHandler.handleTradeWindow}, client đặt tên "fishing" — sai). */
final class TradePackets {

   static final int OPCODE = 56;

   private TradePackets() {
   }

   /** Sub 0: mở cửa sổ giao dịch — str tên, int template gốc, int cấp của bên kia. */
   static Message open(PlayerData partner) {
      Appearance look = partner.appearance;
      Message msg = new Message(OPCODE);
      msg.writeByte(0);
      msg.writeString(partner.name);
      msg.writeInt(look != null ? look.templateId() : Gender.charTemplateId(partner.gender));
      msg.writeInt(partner.level);
      return msg;
   }

   /** Sub 1: bên kia đã khoá — int vàng, byte n + n×item (đúng số lượng đưa ra). */
   static Message partnerOffer(Trade.Offer offer) {
      Message msg = new Message(OPCODE);
      msg.writeByte(1);
      msg.writeInt((int) offer.gold());
      msg.writeByte(offer.size());
      for (int i = 0; i < offer.size(); i++) {
         ItemData shown = offer.originals()[i].copy();
         shown.quantity = offer.quantities()[i];
         ItemPackets.writeItem(msg, shown);
      }
      return msg;
   }

   /** Sub 2: bên kia đã bấm "Đồng ý". */
   static Message partnerAccepted() {
      Message msg = new Message(OPCODE);
      msg.writeByte(2);
      return msg;
   }

   /** Sub 3: đóng cửa sổ giao dịch (xong hoặc huỷ). Client đóng xong gửi lại op 56 sub 3 — server bỏ qua. */
   static Message close() {
      Message msg = new Message(OPCODE);
      msg.writeByte(3);
      return msg;
   }
}
