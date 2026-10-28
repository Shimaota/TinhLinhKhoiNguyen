package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 73 C→S: Top tuần (tab "Top tuần" cửa sổ Cửa hàng, {@code WeeklyTopPanel}). Sub 0 ({@code sendRequestWeeklyTop})
 * int id bảng (= {@code tlnt_data.weekly_top.id}) -> S→C 73/1. Lane GAME (chỉ đọc RAM). Logic: {@link RankingService}.
 */
public final class WeeklyTopHandler implements PacketHandler {

   private final RankingService ranking;

   public WeeklyTopHandler(RankingService ranking) {
      this.ranking = ranking;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         ranking.requestWeekly(session, message.readInt());
      }
   }
}
