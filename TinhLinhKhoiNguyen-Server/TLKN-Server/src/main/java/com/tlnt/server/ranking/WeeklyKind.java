package com.tlnt.server.ranking;

/**
 * 4 bảng Top tuần (tab "Top tuần" cửa sổ Cửa hàng, op 73). {@link #id} = {@code tlnt_data.weekly_top.id} (tên +
 * phần thưởng top 1-3 sửa trong bảng đó); enum chỉ quyết định cách tính điểm. Điểm chỉ tính trong tuần
 * ({@link WeekClock}), thứ Hai 00:00 trả thưởng top 1-3 qua thư rồi đua lại từ 0.
 */
public enum WeeklyKind {

   /** Giàu có = lam thạch nạp trong tuần ({@code WelfareService.addRecharge}). */
   RICH(1, RankingService.W_RECHARGE, " lam thạch"),
   /** Cường hóa = điểm các cấp nâng thành công trong tuần của đồ đang mặc ({@link EnhanceScore#weekly}). */
   ENHANCE(2, RankingService.W_ENHANCE, " điểm"),
   /** Chiến binh = Tinh anh 5 / Thủ lĩnh 10 điểm, hạ trong tuần. */
   WARRIOR(3, RankingService.W_WARRIOR, " điểm"),
   /** Ve chai = vàng nhặt trong tuần. */
   SCAVENGER(4, RankingService.W_GOLD, " vàng");

   public final int id;
   /** Index trong mảng điểm tuần của {@link RankingService.Row}. */
   final int index;
   final String unit;

   WeeklyKind(int id, int index, String unit) {
      this.id = id;
      this.index = index;
      this.unit = unit;
   }

   String format(long value) {
      return RankType.number(value) + unit;
   }

   public static WeeklyKind of(int id) {
      for (WeeklyKind kind : values()) {
         if (kind.id == id) {
            return kind;
         }
      }
      return null;
   }
}
