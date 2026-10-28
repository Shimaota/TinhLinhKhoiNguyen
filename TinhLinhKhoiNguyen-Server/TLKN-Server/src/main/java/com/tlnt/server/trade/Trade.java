package com.tlnt.server.trade;

import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.world.WorldPlayer;

/**
 * 1 phiên giao dịch giữa 2 người. Mọi field đổi được chỉ đọc/ghi khi giữ khoá {@code synchronized (trade)} — 2 người
 * gửi gói trên 2 luồng session khác nhau, khoá này xếp hàng họ lại. Vòng đời:
 * mở (cả 2 {@link PlayerData#trade} trỏ vào đây) -> mỗi bên khoá vật phẩm ({@link Side#offer}) -> mỗi bên đồng ý ->
 * chốt hoặc huỷ ({@link #closed}, gỡ {@link PlayerData#trade}). Đã đóng thì không mở lại.
 */
public final class Trade {

   /** Vật phẩm + vàng 1 bên đưa ra, chụp lúc bấm "Khoá". {@code originals} = chính object trong túi lúc khoá. */
   record Offer(long gold, int[] slots, int[] quantities, ItemData[] originals) {
      int size() {
         return slots.length;
      }
   }

   static final class Side {
      final WorldPlayer player;
      Offer offer;
      boolean accepted;

      Side(WorldPlayer player) {
         this.player = player;
      }

      PlayerData data() {
         return player.data();
      }
   }

   final Side a;
   final Side b;
   final long openedAt;
   boolean closed;

   Trade(WorldPlayer a, WorldPlayer b, long now) {
      this.a = new Side(a);
      this.b = new Side(b);
      this.openedAt = now;
   }

   /** Bên của {@code d}, null nếu không thuộc giao dịch này. */
   Side side(PlayerData d) {
      return a.data() == d ? a : b.data() == d ? b : null;
   }

   Side other(Side side) {
      return side == a ? b : a;
   }
}
