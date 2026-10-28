package com.tlnt.server.fishing;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packet;

/**
 * Gói câu cá. S→C op 57 = client {@code ServerPacketHandler.handleGatherAction} (tên sai); chế độ HUD = op 2 sub 4.
 */
public final class FishingPackets {

   /** Op 2 sub 4: index trong {@code MoveSpeedTable.VALUES} — 0 thường, 2 câu cá. */
   public static final int HUD_NORMAL = 0;
   public static final int HUD_FISHING = 2;

   private FishingPackets() {
   }

   /**
    * Op 2 sub 4 ({@code handlePlayerUpdate}): chế độ HUD của chính mình. Câu cá = hiện nút tròn "nắm tay"
    * ({@code AvatarButton}, bấm -> C→S op 57 sub 0), ẩn thanh kỹ năng + nút nhặt.
    */
   public static Message hudMode(boolean fishing) {
      Message msg = new Message(Opcode.PLAYER);
      msg.writeByte(4);
      msg.writeByte(fishing ? HUD_FISHING : HUD_NORMAL);
      return msg;
   }

   /** Op 57 sub 0: {@code playerId} quăng cần (anim rod/fishing_1, xong tự lặp rod/fishing_2 — chờ cá). */
   static Packet cast(int playerId) {
      Message msg = new Message(Opcode.FISHING);
      msg.writeByte(0);
      msg.writeInt(playerId);
      return msg.toPacket();
   }

   /**
    * Op 57 sub 1: {@code playerId} kéo được cá — byte n, n × int mẫu item (icon bay lên trên đầu), anim rod/fishing_3
    * rồi đứng yên; client của chính người đó ẩn thanh kéo cần.
    */
   static Packet caught(int playerId, int itemTemplateId) {
      Message msg = new Message(Opcode.FISHING);
      msg.writeByte(1);
      msg.writeInt(playerId);
      msg.writeByte(1);
      msg.writeInt(itemTemplateId);
      return msg.toPacket();
   }

   /**
    * Op 57 sub 2 (chỉ gửi người câu): hiện thanh kéo cần. int ms đã trôi (client lùi mốc thời gian), float vàng
    * đầu / cuối, float xanh đầu / cuối (0..1 theo bề rộng thanh), int ms mỗi lượt kim chạy.
    */
   static Message bar(Fishing f) {
      Message msg = new Message(Opcode.FISHING);
      msg.writeByte(2);
      msg.writeInt(0);
      msg.writeFloat(f.yellowStart);
      msg.writeFloat(f.yellowEnd);
      msg.writeFloat(f.greenStart);
      msg.writeFloat(f.greenEnd);
      msg.writeInt(f.period);
      return msg;
   }

   /** Op 57 sub 3: {@code playerId} thôi câu (về đứng yên; client của chính người đó ẩn thanh kéo cần). */
   static Packet stop(int playerId) {
      Message msg = new Message(Opcode.FISHING);
      msg.writeByte(3);
      msg.writeInt(playerId);
      return msg.toPacket();
   }
}
