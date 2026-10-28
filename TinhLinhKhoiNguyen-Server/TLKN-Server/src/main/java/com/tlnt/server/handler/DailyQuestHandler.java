package com.tlnt.server.handler;

import com.tlnt.server.daily.DailyQuestService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 69 C→S: nhiệm vụ hằng ngày (tab "Hàng ngày" {@code DailyQuestPanel}). Tên method client sai:
 *
 * <pre>
 * sub | method client       | ý nghĩa thật
 * 0   | sendRequestMount    | mở tab (onShow)
 * 1   | sendPetSummon(id)   | "Nhận" quà mốc chăm chỉ (int id mốc)
 * 2   | sendPetDismiss(i)   | "Đổi NV" — NV chưa xong (int index)
 * 3   | sendPetInteract(i)  | "Nhận" thưởng NV đã xong (int index)
 * 4   | sendPetStay         | "Nhận NV mới"
 * </pre>
 * Logic: {@link DailyQuestService}.
 */
public final class DailyQuestHandler implements PacketHandler {

   private final DailyQuestService daily;

   public DailyQuestHandler(DailyQuestService daily) {
      this.daily = daily;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> daily.open(session);
         case 1 -> daily.claimMilestone(session, message.readInt());
         case 2 -> daily.askSwap(session, message.readInt());
         case 3 -> daily.claim(session, message.readInt());
         case 4 -> daily.askAdd(session);
         default -> {
         }
      }
   }
}
