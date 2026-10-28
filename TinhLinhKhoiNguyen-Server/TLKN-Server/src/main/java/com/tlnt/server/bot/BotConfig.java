package com.tlnt.server.bot;

/**
 * Cài đặt cơ bản của người chơi giả — 1 dòng {@code tlnt_data.bot_profile} (id 1, {@link BotRepository}); sửa ở admin panel
 * tab "Bot người chơi" hoặc "Dữ liệu game" > Bot người chơi, server đọc lại mỗi phút. Bất biến; đổi = thay cả bản.
 *
 * <p>Tỉ lệ loại bot: {@code grindPercent}% đi up (trong đó {@code autoPercent}% kiểu auto đánh, còn lại kiểu treo
 * offline), {@code fishPercent}% câu cá, {@code afkPercent}% AFK, phần còn lại đứng chơi / đi dạo. Tổng 3 phần đầu > 100
 * thì kẹp theo thứ tự. {@code *Chance} = % bot có 1 món bộ sưu tập loại đó (lấy từ danh sách trong cùng dòng).
 *
 * @param partyPercent   % bot up vào game theo nhóm 2-4 người cùng bãi (tự lập tổ đội)
 * @param bossHunters    số bot kéo tới mỗi boss thế giới đang sống (0 = tắt) — thêm ngoài {@code count}
 * @param zonePercent    mức trần bot theo % sức chứa khu; từng khu lấy trần ổn định ngẫu nhiên từ 50–100% mức này
 * @param sessionMin     1 bot online ngẫu nhiên trong khoảng phút này rồi thoát, bot mới (tên khác) vào thay
 * @param pkFightBack    bị người chơi PK thì đánh trả (đủ sức) / bỏ chạy
 * @param spawnPerSecond tối đa bấy nhiêu bot vào / ra mỗi giây
 */
public record BotConfig(boolean enabled, int count, int grindPercent, int autoPercent, int fishPercent, int afkPercent,
      int partyPercent, int bossHunters, int minLevel, int maxLevel, int zonePercent, int sessionMin, int sessionMax,
      boolean chat, boolean pkFightBack, int spawnPerSecond, int costumeChance, int titleChance, int petChance,
      int maskChance, int backChance) {

   public static final int MAX_COUNT = 5_000;

   public static BotConfig defaults() {
      return new BotConfig(false, 200, 60, 80, 8, 7, 25, 4, 5, 60, 50, 30, 150, true, true, 5, 25, 20, 10, 8, 5);
   }

   /** Kẹp mọi giá trị vào khoảng hợp lệ. */
   public BotConfig normalized() {
      int lo = clamp(minLevel, 1, 100);
      int hi = clamp(maxLevel, lo, 100);
      int sMin = clamp(sessionMin, 5, 24 * 60);
      int sMax = clamp(sessionMax, sMin, 24 * 60);
      int g = clamp(grindPercent, 0, 100);
      int f = clamp(fishPercent, 0, 100 - g);
      int k = clamp(afkPercent, 0, 100 - g - f);
      return new BotConfig(enabled, clamp(count, 0, MAX_COUNT), g, clamp(autoPercent, 0, 100), f, k,
            clamp(partyPercent, 0, 100), clamp(bossHunters, 0, 20), lo, hi, clamp(zonePercent, 5, 90), sMin, sMax, chat,
            pkFightBack, clamp(spawnPerSecond, 1, 50), clamp(costumeChance, 0, 100), clamp(titleChance, 0, 100),
            clamp(petChance, 0, 100), clamp(maskChance, 0, 100), clamp(backChance, 0, 100));
   }

   /** % bot đứng chơi / đi dạo (phần còn lại). */
   public int idlePercent() {
      return Math.max(0, 100 - grindPercent - fishPercent - afkPercent);
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
