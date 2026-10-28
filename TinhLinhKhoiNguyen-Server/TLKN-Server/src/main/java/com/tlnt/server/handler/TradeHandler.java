package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.trade.TradeService;

/**
 * Opcode 56 C→S: giao dịch (client đặt tên "fishing" — sai). Logic + luật chống nhân bản: {@link TradeService}.
 *
 * <pre>
 * sub | method client        | ý nghĩa thật                         | payload
 * 0   | sendFishingAction    | "Giao dịch" trong menu người chơi    | int playerId
 * 1   | sendFishingResult    | "Khoá" vật phẩm                      | int vàng, byte n, n×(int ô túi, int số lượng)
 * 2   | sendStopFishing      | "Đồng ý"                             | —
 * 3   | sendRequestFishing   | đóng cửa sổ giao dịch = huỷ          | —
 * </pre>
 */
public final class TradeHandler implements PacketHandler {

   private final TradeService trades;

   public TradeHandler(TradeService trades) {
      this.trades = trades;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> trades.invite(session, message.readInt());
         case 1 -> lock(session, message);
         case 2 -> trades.accept(session);
         case 3 -> trades.cancel(session);
         default -> {
         }
      }
   }

   private void lock(PlayerSession session, Message message) {
      long gold = message.readInt();
      int count = message.readByte();
      if (count < 0) { // gói hỏng / giả: huỷ luôn cho chắc
         trades.cancel(session);
         return;
      }
      int[] slots = new int[count];
      int[] quantities = new int[count];
      for (int i = 0; i < count; i++) {
         slots[i] = message.readInt();
         quantities[i] = message.readInt();
      }
      trades.lock(session, gold, slots, quantities);
   }
}
