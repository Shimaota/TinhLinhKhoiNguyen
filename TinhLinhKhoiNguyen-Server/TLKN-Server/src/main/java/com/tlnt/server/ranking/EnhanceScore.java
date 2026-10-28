package com.tlnt.server.ranking;

import com.tlnt.server.forge.UpgradeRules;
import com.tlnt.server.player.ItemData;

import java.util.List;

/**
 * Điểm Cường hóa: mỗi cấp nâng THÀNH CÔNG = số đá bậc đó × {@link #POINTS_PER_STONE} (bảng {@link UpgradeRules#step}:
 * cấp 1 = 50 đá -> 500 điểm, cấp 2 = 300 đá -> 3.000, cấp 3 = 2.400 đá -> 24.000...). Món cấp N = tổng điểm cấp 1..N.
 * CHỈ tính đồ đang mặc (10 ô {@code PlayerData.body}) — nâng xong phải mặc vào mới có điểm.
 */
public final class EnhanceScore {

   public static final int POINTS_PER_STONE = 10;
   /** CUMULATIVE[n] = điểm của món cấp n. */
   private static final long[] CUMULATIVE = new long[UpgradeRules.MAX_LEVEL + 1];

   static {
      for (int level = 1; level <= UpgradeRules.MAX_LEVEL; level++) {
         CUMULATIVE[level] = CUMULATIVE[level - 1] + (long) UpgradeRules.step(level - 1).stones() * POINTS_PER_STONE;
      }
   }

   private EnhanceScore() {
   }

   /** Điểm 1 món cấp {@code level}. */
   public static long of(int level) {
      return CUMULATIVE[Math.max(0, Math.min(UpgradeRules.MAX_LEVEL, level))];
   }

   /** Tổng điểm đồ đang mặc. */
   public static long total(List<ItemData> worn) {
      long sum = 0;
      for (ItemData item : worn) {
         if (item != null) {
            sum += of(item.starCount);
         }
      }
      return sum;
   }

   /** Điểm các cấp đạt được TRONG tuần {@code weekId} của đồ đang mặc (giảm cấp trong tuần thì trừ lại, không âm). */
   public static long weekly(List<ItemData> worn, int weekId) {
      long sum = 0;
      for (ItemData item : worn) {
         if (item != null && weekId != 0 && item.upgradeWeek == weekId) {
            sum += Math.max(0, of(item.starCount) - of(item.upgradeWeekBase));
         }
      }
      return sum;
   }

   /** Gọi NGAY TRƯỚC mỗi lần nâng (thành công hay thất bại): lần đầu trong tuần thì ghi lại cấp đầu tuần. */
   public static void markUpgrade(ItemData item, int weekId) {
      if (item.upgradeWeek != weekId) {
         item.upgradeWeek = weekId;
         item.upgradeWeekBase = item.starCount;
      }
   }
}
