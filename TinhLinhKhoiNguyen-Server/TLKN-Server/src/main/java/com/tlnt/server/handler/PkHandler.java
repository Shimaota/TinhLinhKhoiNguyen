package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.PkRules;
import com.tlnt.server.social.SocialService;

/**
 * Opcode 42 C→S sub 0 ({@code sendGuildInvite}, tên sai; byte = {@code FactionType.ordinal()}): chọn trạng thái PK ở
 * menu biểu tượng trên HUD ({@code PopupMenu}). Luật: {@link PkRules}.
 */
public final class PkHandler implements PacketHandler {

   private final SocialService social;

   public PkHandler(SocialService social) {
      this.social = social;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         social.setFaction(session, message.readByte());
      }
   }
}
