package com.tlnt.server.farm;

import com.tlnt.server.net.Message;

/** Gói S→C nông trại (op 52 — client {@code ServerPacketHandler.handleAnimalUpdate}). */
public final class FarmPackets {

   public static final int OPCODE = 52;

   private FarmPackets() {
   }

   /**
    * Sub 2: trạng thái công trình nông trại ({@code GameState.buildingStatus}) — chữ trên đầu NPC 18 Cây táo
    * ({@code NpcBuildingStatusLabel}): "Cây táo cấp x" + "Có thể thu hoạch (n/max) mm:ss" hoặc "Đang nâng cấp (...)".
    * Client tự đếm giờ từ các mốc này.
    *
    * <p>int cấp, bool đang nâng cấp; không nâng cấp: long ms đã ra quả, int quả tối đa, int ms / quả; đang nâng cấp:
    * long ms đã nâng, long tổng ms nâng cấp.
    */
   public static Message appleTree(AppleTree tree, long now) {
      Message msg = new Message(OPCODE);
      msg.writeByte(2);
      msg.writeInt(tree.level());
      msg.writeBoolean(tree.upgrading());
      if (tree.upgrading()) {
         msg.writeLong(Math.max(0, now - tree.upgradeStart()));
         msg.writeLong(tree.upgradeEnd() - tree.upgradeStart());
      } else {
         msg.writeLong(Math.max(0, now - tree.productionStart()));
         msg.writeInt(AppleTree.maxFruits(tree.level()));
         msg.writeInt((int) AppleTree.intervalMs(tree.level()));
      }
      return msg;
   }
}
