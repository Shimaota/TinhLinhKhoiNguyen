package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.ShopService;

/**
 * Opcode 55 C→S: mua ở cửa hàng NPC. Sub 0 ({@code sendEnhanceEquipment} — tên sai; nút "Mua" / "Mua nhiều" trong
 * {@code MarketSellWindow}): int shopId, int tab, int ô, int số lần. Sub 1 ({@code sendRequestEnhance} — tên sai; mở tab
 * "Cửa hàng" của cửa sổ Bộ lạc {@code TribeShopPanel}): không payload. Logic: {@link ShopService}.
 */
public final class ShopHandler implements PacketHandler {

   private final ShopService shops;

   public ShopHandler(ShopService shops) {
      this.shops = shops;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> {
            int shopId = message.readInt();
            int tab = message.readInt();
            int slot = message.readInt();
            int times = message.readInt();
            shops.buy(session, shopId, tab, slot, times);
         }
         case 1 -> shops.openTribe(session);
         default -> {
         }
      }
   }
}
