package com.tlnt.server.handler;

import com.tlnt.server.combo.ComboService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 68 C→S: gói combo (tab "Gói combo" của {@code ShopWindow}). Logic: {@link ComboService}.
 *
 * <pre>
 * sub | method client (tên sai)  | nút               | payload
 * 0   | sendRequestSocialList    | mở tab            | -
 * 1   | sendSocialAction         | "Mua" 1 gói       | int id danh mục, int số thứ tự gói
 * </pre>
 */
public final class ComboHandler implements PacketHandler {

   private final ComboService combos;

   public ComboHandler(ComboService combos) {
      this.combos = combos;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> combos.open(session);
         case 1 -> {
            int comboId = message.readInt();
            combos.buy(session, comboId, message.readInt());
         }
         default -> {
         }
      }
   }
}
