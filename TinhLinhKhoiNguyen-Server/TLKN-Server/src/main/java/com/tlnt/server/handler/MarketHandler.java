package com.tlnt.server.handler;

import com.tlnt.server.market.MarketService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 36 C→S: kí gửi (cửa sổ {@code MarketWindow}, nút HUD). Logic + luật: {@link MarketService}.
 *
 * <pre>
 * sub | method client (tên sai) | nút                                          | payload
 * 0   | sendRequestStats        | mở cửa sổ                                    | —
 * 1   | sendBuyItem             | "Bán" (tab Ký gửi > Bán)                     | int ô túi, int số lượng, int giá, byte dòng market_fee
 * 2   | sendRequestTrade        | "Mua" (tab Cửa hàng, BuyItemAction)          | int listingId (= item.slotIndex)
 * 3   | sendTradeAction         | "Gỡ xuống" (tab Đang bán)                    | int listingId
 * 4   | sendConfirmTrade        | "Rao bán" (tab Đang bán)                     | int listingId
 * </pre>
 */
public final class MarketHandler implements PacketHandler {

   private final MarketService market;

   public MarketHandler(MarketService market) {
      this.market = market;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> market.open(session);
         case 1 -> {
            int slot = message.readInt();
            int quantity = message.readInt();
            int price = message.readInt();
            int feeIndex = message.readByte();
            market.sell(session, slot, quantity, price, feeIndex);
         }
         case 2 -> market.buy(session, message.readInt());
         case 3 -> market.cancel(session, message.readInt());
         case 4 -> market.relist(session, message.readInt());
         default -> {
         }
      }
   }
}
