package com.tlnt.server.handler;

import com.tlnt.server.forge.WeaponCraftService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 60 C→S: "Chế Tác Vũ Khí". Sub 0 ({@code sendMountAction} — tên sai; nút "Chế tác" của
 * {@code WeaponCraftWindow}): byte vị trí công thức. Logic: {@link WeaponCraftService}.
 */
public final class WeaponCraftHandler implements PacketHandler {

   private final WeaponCraftService crafts;

   public WeaponCraftHandler(WeaponCraftService crafts) {
      this.crafts = crafts;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         crafts.craft(session, message.readByte());
      }
   }
}
