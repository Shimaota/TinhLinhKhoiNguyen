package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.worldboss.WorldBossList;

/**
 * Opcode 100 C→S (MỚI — bảng Boss thế giới, client {@code ServerPacketSender.sendWorldBoss*}):
 *
 * <pre>
 * sub | payload  | xử lý
 * 0   | —        | xin danh sách -> S→C 100/0 (client hỏi lại mỗi 3 s khi đang mở bảng)
 * 1   | int id   | "Dịch chuyển" (5 lam thạch) -> hỏi op 32 loại 19
 * 2   | int id   | "Tìm đường" (100.000 vàng) -> hỏi op 32 loại 20
 * </pre>
 */
public final class WorldBossHandler implements PacketHandler {

   private final WorldBossList list;

   public WorldBossHandler(WorldBossList list) {
      this.list = list;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      switch (message.readByte()) {
         case 0 -> list.list(session);
         case 1 -> list.requestTeleport(session, message.readInt());
         case 2 -> list.requestPath(session, message.readInt());
         default -> {
         }
      }
   }
}
