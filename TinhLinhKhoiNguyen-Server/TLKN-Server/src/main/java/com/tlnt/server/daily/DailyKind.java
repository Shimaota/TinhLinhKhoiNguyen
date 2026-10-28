package com.tlnt.server.daily;

/**
 * Loại việc của 1 nhiệm vụ hằng ngày (khoá {@code "kind"} trong {@code tlnt_data.daily_quest} dòng {@code tasks}).
 * {@code target} của nhiệm vụ nghĩa khác nhau theo loại (ghi ở từng hằng); 0 = không cần.
 */
public enum DailyKind {
   /** Hồi sinh (Cỏ hồi sinh tại chỗ hoặc về làng). */
   REVIVE("revive"),
   /**
    * Nhặt / hái được vật phẩm mẫu {@code target} (đồ dưới đất + hái cây), tính theo SỐ LƯỢNG; {@code target} 1 = vàng
    * nhặt dưới đất (số vàng).
    */
   PICK("pick"),
   /** Hạ quái mẫu {@code target} (mọi biến thể); 0 trong cấu hình = chọn quái hợp cấp lúc giao ({@link DailyMobs}). */
   KILL("kill"),
   /** Hạ Tinh anh / Thủ lĩnh của quái mẫu {@code target}; 0 = chọn quái hợp cấp có Tinh anh. */
   KILL_ELITE("kill_elite"),
   /** Đòn gây sát thương lên boss thế giới {@code target} ({@code world_boss.id}). */
   BOSS_HIT("boss_hit"),
   /** Mua ở shop NPC vật phẩm mẫu {@code target} (số lượng). */
   BUY("buy"),
   /** Thu hoạch Heo trưởng thành — CHƯA có (nông trại chưa nuôi heo): giao được nhưng chưa làm xong được. */
   PIG("pig"),
   /** Tinh luyện Đá khởi nguyên (số đá). */
   REFINE("refine"),
   /** Niêm yết 1 món lên Ký gửi. */
   MARKET_SELL("market_sell"),
   /** Hạ Tiểu Tinh Linh Cáo — CHƯA có boss này: giao được nhưng chưa làm xong được. */
   FOX("fox"),
   /** Hạ gục người chơi khác (ngoài thách đấu) hoặc thắng thách đấu. */
   PVP("pvp"),
   /** Thu hoạch Cây táo (số quả). */
   APPLE("apple");

   public final String key;

   DailyKind(String key) {
      this.key = key;
   }

   /** Bit của loại trong {@link DailyState#watch} (lọc nhanh trên luồng tick khu). */
   long bit() {
      return 1L << ordinal();
   }

   public static DailyKind of(String key) {
      for (DailyKind k : values()) {
         if (k.key.equals(key)) {
            return k;
         }
      }
      return null;
   }
}
