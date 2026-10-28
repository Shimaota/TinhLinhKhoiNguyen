package com.tlnt.server.handler;

import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

/**
 * Opcode 22 C→S (không payload):
 * <pre>
 * sub | method client (tên sai) | nút
 * 0   | sendGuildLeave          | "Sắp xếp" hành trang (InventoryGridPanel.SortButton)
 * 1   | sendGuildKick           | "Sắp xếp" rương (ItemCountGridPanel.ConfirmButton)
 * </pre>
 */
public final class InventoryHandler implements PacketHandler {

   private final ItemService items;

   public InventoryHandler(ItemService items) {
      this.items = items;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      byte sub = message.readByte();
      switch (sub) {
         case 0 -> items.sortBag(session);
         case 1 -> items.sortBox(session);
         default -> Log.debug("Session #%d gui opcode 22 sub %d chua ho tro", session.getId(), sub);
      }
   }
}
