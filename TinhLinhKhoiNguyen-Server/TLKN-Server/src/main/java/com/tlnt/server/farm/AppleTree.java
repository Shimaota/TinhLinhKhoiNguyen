package com.tlnt.server.farm;

/**
 * Cây táo ở Nông trại (NPC 18, map riêng 5 — mỗi nhân vật 1 cây). Bất biến: mọi thay đổi tạo bản mới rồi gán vào
 * {@link FarmState#apple} (volatile) -> luồng tick khu đọc để gửi trạng thái không cần khoá.
 *
 * <p>Ra quả: cứ {@link #intervalMs} ra 1 quả kể từ {@code productionStart}, tối đa {@link #maxFruits} — y hệt công
 * thức client vẽ trên đầu cây ({@code NpcBuildingStatusLabel}). Đang nâng cấp ({@code upgradeEnd > 0}): số quả
 * đứng yên ở {@code frozen} (số quả lúc bấm nâng cấp), không ra thêm, không thu hoạch; tới {@code upgradeEnd} tự lên cấp
 * ({@link #settle}), giữ {@code frozen} quả và ra tiếp theo tốc độ cấp mới.
 *
 * @param level           cấp 1..{@link #MAX_LEVEL}
 * @param productionStart mốc (epoch ms) bắt đầu đếm quả
 * @param upgradeStart    mốc bắt đầu nâng cấp (0 = không nâng cấp)
 * @param upgradeEnd      mốc nâng cấp xong (0 = không nâng cấp)
 * @param frozen          số quả giữ trên cây trong lúc nâng cấp
 */
public record AppleTree(int level, long productionStart, long upgradeStart, long upgradeEnd, int frozen) {

   public static final int MAX_LEVEL = 10;

   private static final long MINUTE = 60_000L;
   private static final long DAY = 24 * 60 * MINUTE;

   /** Theo cấp (chỉ số = cấp, ô 0 bỏ trống): số quả tối đa, phút / quả. */
   private static final int[] MAX_FRUITS = {0, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
   private static final int[] MINUTES_PER_FRUIT = {0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4};
   /** Nâng từ cấp {@code i} lên {@code i + 1}: vàng, thời gian, lam thạch nâng nhanh. */
   private static final long[] UPGRADE_GOLD = {0, 100_000, 600_000, 1_500_000, 35_000_000, 50_000_000, 65_000_000,
         80_000_000, 95_000_000, 100_000_000};
   private static final long[] UPGRADE_MS = {0, 5 * MINUTE, 30 * MINUTE, 3 * DAY, 7 * DAY, 20 * DAY, 30 * DAY,
         40 * DAY, 50 * DAY, 60 * DAY};
   private static final int[] QUICK_DIAMOND = {0, 10, 50, 100, 200, 250, 300, 350, 400, 500};

   /** Cây mới (nhân vật mới / lần đầu vào Nông trại): cấp 1, ĐẦY quả sẵn (5/5) — hái được ngay cho nhiệm vụ 1. */
   public static AppleTree fresh(long now) {
      return new AppleTree(1, now - maxFruits(1) * intervalMs(1), 0, 0, 0);
   }

   public static int maxFruits(int level) {
      return MAX_FRUITS[clamp(level)];
   }

   public static long intervalMs(int level) {
      return MINUTES_PER_FRUIT[clamp(level)] * MINUTE;
   }

   public static long upgradeGold(int level) {
      return UPGRADE_GOLD[clamp(level)];
   }

   public static long upgradeMs(int level) {
      return UPGRADE_MS[clamp(level)];
   }

   public static int quickDiamond(int level) {
      return QUICK_DIAMOND[clamp(level)];
   }

   private static int clamp(int level) {
      return Math.max(1, Math.min(MAX_LEVEL, level));
   }

   public boolean upgrading() {
      return upgradeEnd > 0;
   }

   public boolean maxLevel() {
      return level >= MAX_LEVEL;
   }

   /** Trạng thái thật lúc {@code now}: nâng cấp đã tới giờ thì lên cấp, ra quả lại từ mốc xong. Dữ liệu hỏng -> sửa. */
   public AppleTree settle(long now) {
      if (level < 1 || level > MAX_LEVEL) {
         return new AppleTree(clamp(level), productionStart, 0, 0, 0);
      }
      if (upgrading() && now >= upgradeEnd) {
         return levelUp(upgradeEnd);
      }
      return this;
   }

   /** Số quả đang có trên cây (đang nâng cấp = số quả giữ lại lúc bắt đầu nâng). */
   public int fruits(long now) {
      if (upgrading()) {
         return frozen;
      }
      long elapsed = Math.max(0, now - productionStart);
      return (int) Math.min(maxFruits(level), elapsed / intervalMs(level));
   }

   /**
    * Hái hết quả: cây chưa đầy thì giữ phần thời gian đang dở của quả kế tiếp, đầy rồi thì đếm lại từ {@code now}
    * (lúc đầy cây ngừng ra quả).
    */
   public AppleTree harvested(long now) {
      long elapsed = Math.max(0, now - productionStart);
      long start = fruits(now) >= maxFruits(level) ? now : now - elapsed % intervalMs(level);
      return new AppleTree(level, start, 0, 0, 0);
   }

   /** Bắt đầu nâng cấp: quả đang có đứng yên tới lúc nâng xong (phần giờ dở của quả kế tiếp bỏ). */
   public AppleTree upgradeStarted(long now) {
      return new AppleTree(level, productionStart, now, now + upgradeMs(level), fruits(now));
   }

   /** Nâng cấp nhanh: lên cấp ngay. */
   public AppleTree upgradeFinished(long now) {
      return levelUp(now);
   }

   /** Lên cấp lúc {@code at}: còn {@code frozen} quả, quả kế tiếp ra sau 1 chu kỳ của cấp mới. */
   private AppleTree levelUp(long at) {
      int next = Math.min(MAX_LEVEL, level + 1);
      int kept = Math.min(frozen, maxFruits(next));
      return new AppleTree(next, at - kept * intervalMs(next), 0, 0, 0);
   }
}
