package com.tlnt.server.achievement;

/**
 * Loại sự kiện 1 chuỗi thành tựu đếm (cột {@code tlnt_data.achievement.category}). Mỗi loại 1 bộ đếm chung cho cả chuỗi
 * (game gốc: "Hồi sinh 1.000 lần" hiện 126/1000 cùng số với "Hồi sinh 5.000 lần").
 *
 * <p>{@link #max} = bộ đếm giữ GIÁ TRỊ CAO NHẤT từng đạt (cấp độ, số sao cường hóa), thành tựu dùng {@code param} làm
 * mốc và hiện 0/1 -> 1/1 (như game gốc); còn lại = cộng dồn, hiện min(đếm, mục tiêu)/mục tiêu.
 */
public enum AchievementType {
   /** Hạ quái Tinh anh (người kết liễu). */
   ELITE("elite", false),
   /** Hạ quái Thủ lĩnh (người kết liễu). */
   LEADER("leader", false),
   /** Hạ boss thế giới "Vệ thần" (người kết liễu; khoá achievement.guardianBosses). */
   GUARDIAN("guardian", false),
   /** Hạ boss thế giới "Tinh linh" (người kết liễu; khoá achievement.spiritBosses). */
   SPIRIT("spirit", false),
   /** Hồi sinh bằng Cỏ hồi sinh. */
   REVIVE("revive", false),
   /** Niêm yết 1 món lên Ký gửi. */
   MARKET_SELL("market_sell", false),
   /** Pha chế thuốc Ma thuật thành công (Olivar). */
   BREW("brew", false),
   /** Mở Rương hoạt động (item 172, {@code item/UsableItems}). */
   ACTIVITY_CHEST("activity_chest", false),
   /** Số sao cao nhất từng đạt khi NÂNG CẤP THÀNH CÔNG (không quét đồ trong túi / rương). */
   UPGRADE_STAR("upgrade_star", true),
   /** Số Đá khởi nguyên đã tinh luyện (Luyện đá). */
   REFINE_STONE("refine_stone", false),
   /** Câu được cá (không tính câu hụt kéo lên rác). */
   FISH("fish", false),
   /** Phá tổ ong (người phá). */
   BEEHIVE("beehive", false),
   /** Thu hoạch Heo trưởng thành — CHƯA có (nông trại chưa nuôi heo). */
   PIG_HARVEST("pig_harvest", false),
   /** Cấp độ cao nhất. */
   LEVEL("level", true);

   public final String key;
   public final boolean max;

   AchievementType(String key, boolean max) {
      this.key = key;
      this.max = max;
   }

   public static AchievementType of(String key) {
      for (AchievementType t : values()) {
         if (t.key.equals(key)) {
            return t;
         }
      }
      return null;
   }
}
