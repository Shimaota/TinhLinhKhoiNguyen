package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.npc.NpcService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 24 C→S: NPC. Tên method client sai:
 *
 * <pre>
 * sub | method client     | ý nghĩa thật                                       | payload
 * 0   | sendUnequipItem   | bấm NPC (client đã chạy tới cạnh — MoveToNpcCallback) | byte id thực thể, int npc_template.id
 * 1   | sendMoveItem      | bấm nút hộp thoại NPC (client tự đóng hộp thoại)    | byte trang, int npc_template.id, byte nút
 * </pre>
 * Id thực thể gửi bằng BYTE -> đọc không dấu (0..255).
 */
public final class NpcHandler implements PacketHandler {

   private final NpcService npcs;

   public NpcHandler(NpcService npcs) {
      this.npcs = npcs;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> {
            int npcIndex = message.readByte() & 0xFF;
            npcs.talk(session, npcIndex, message.readInt());
         }
         case 1 -> {
            int page = message.readByte();
            int templateId = message.readInt(); // id server gửi trong hộp thoại (op 24 sub 1) = mẫu NPC
            npcs.choose(session, page, templateId, message.readByte());
         }
         default -> {
         }
      }
   }
}
