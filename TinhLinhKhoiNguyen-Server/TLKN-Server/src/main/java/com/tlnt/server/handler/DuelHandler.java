package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.SocialService;

/**
 * Opcode 41 C→S sub 0 ({@code sendPartySkill}, tên sai; int playerId): "Thách thức" trong menu người chơi
 * ({@code UnlockItemContextAction}) -> lời mời thách đấu. Đồng ý đi qua opcode 32 loại 3 ({@link ConfirmHandler}).
 */
public final class DuelHandler implements PacketHandler {

   private final SocialService social;

   public DuelHandler(SocialService social) {
      this.social = social;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         social.challenge(session, message.readInt());
      }
   }
}
