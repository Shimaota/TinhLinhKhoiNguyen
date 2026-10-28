package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 39 C→S: bảng xếp hạng. Sub 0 ({@code sendRequestRanking}) int id bảng -> S→C 39/1. Lane GAME (chỉ đọc
 * RAM). Logic: {@link RankingService}.
 */
public final class RankingHandler implements PacketHandler {

   private final RankingService ranking;

   public RankingHandler(RankingService ranking) {
      this.ranking = ranking;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         ranking.request(session, message.readInt());
      }
   }
}
