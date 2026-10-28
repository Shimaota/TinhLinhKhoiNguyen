package com.tlnt.server.forge;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.player.ItemData;

/**
 * Luật nâng cấp (cường hoá) trang bị ở thợ rèn (Ông Morgi -> ForgeWindow tab "Nâng cấp", op 44 chế độ 0).
 *
 * <ul>
 *   <li>Trang bị nâng được: loại mặc được có {@code equip_slot} 0..7 (áo, quần, găng, giày, vũ khí, đai, mũ, vòng cổ —
 *       {@code item_type} 3..10 và 40..47).</li>
 *   <li>Đá: {@link #SUN_STONE} Đá Mặt trời / {@link #MOON_STONE} Đá Mặt trăng. Lần nâng ĐẦU (cấp 0) quyết định hệ của
 *       món: đá mặt trời -> Nhật ấn ({@code starGrade} 1), đá mặt trăng -> Nguyệt ấn (2); từ đó chỉ nâng bằng đá cùng
 *       hệ.</li>
 *   <li>Tỉ lệ = tỉ lệ tối đa × (số đá bỏ vào / số đá yêu cầu), tối đa bằng tỉ lệ tối đa. Vd cần 1000 viên đạt 50%,
 *       bỏ 500 viên -> 25%. Thành công hay thất bại đều mất đá + vàng.</li>
 *   <li>Thất bại: trang bị từ cấp 2 trở lên mà không có bảo hiểm -> giảm 1 cấp. Bùa bảo hiểm cấp 1 ({@link #INSURANCE_1})
 *       chỉ dùng khi trang bị dưới 5 sao; cấp 2 ({@link #INSURANCE_2}) dùng mọi cấp.</li>
 *   <li>Chỉ số: mỗi cấp +{@link #PERCENT_PER_LEVEL}% chỉ số GỐC (tuyến tính), chỉ số dạng % (định dạng stat_type có
 *       "%") không tăng. Item chỉ lưu chỉ số gốc + cấp; giá trị theo cấp tính lúc gửi client / cộng vào nhân vật
 *       ({@link #scaled}) -> giảm cấp là về đúng chỉ số của cấp trước.</li>
 * </ul>
 */
public final class UpgradeRules {

   public static final int SUN_STONE = 125;
   public static final int MOON_STONE = 126;
   public static final int INSURANCE_1 = 169;
   public static final int INSURANCE_2 = 170;
   /**
    * Bùa bảo hiểm Tinh linh: mọi cấp trang bị, thất bại không bị giảm cấp VÀ giữ lại tài nguyên (đá + vàng không bị trừ).
    * Mất 1 bùa mỗi lần nâng (như bùa cấp 1 / 2).
    */
   public static final int INSURANCE_SPIRIT = 310;
   /**
    * Ngọc trai đen: đặt ở ô bảo hiểm, chỉ khi trang bị từ cấp {@link #PEARL_MIN_LEVEL} (nâng 3 -> 4 trở lên), tiêu hao
    * mỗi lần nâng. Thành công: cấp mới +{@link #PEARL_PERCENT}% thay vì {@link #PERCENT_PER_LEVEL}%; thất bại: về cấp 1
    * (chỉ số cấp 1 giữ như lúc nâng lên cấp 1). Tỉ lệ thành công không đổi.
    */
   public static final int BLACK_PEARL = 296;
   public static final int PEARL_MIN_LEVEL = 3;
   public static final int PEARL_PERCENT = 12;
   public static final int PEARL_FAIL_LEVEL = 1;
   /** Bùa bảo hiểm cấp 1 chỉ dùng khi trang bị DƯỚI bấy nhiêu sao. */
   public static final int INSURANCE_1_MAX_LEVEL = 5;
   /** Thất bại từ cấp này trở lên (không bảo hiểm) thì giảm 1 cấp. */
   public static final int DOWNGRADE_FROM_LEVEL = 2;
   public static final int MAX_LEVEL = 10;
   public static final int PERCENT_PER_LEVEL = 4;

   /** {@code Item.StarGrade} client: 0 chưa có, 1 Nhật ấn, 2 Nguyệt ấn. */
   public static final byte GRADE_NONE = 0;
   public static final byte GRADE_SUN = 1;
   public static final byte GRADE_MOON = 2;

   /**
    * 1 bậc nâng (từ cấp {@code index} lên cấp kế).
    *
    * @param ratePermille tỉ lệ thành công tối đa (phần nghìn)
    * @param stones số đá để đạt tỉ lệ tối đa
    */
   public record Step(long gold, int ratePermille, int stones) {
   }

   private static final Step[] STEPS = {
      new Step(100_000L, 1000, 50), // 0 -> 1
      new Step(500_000L, 800, 300), // 1 -> 2
      new Step(2_500_000L, 750, 2_400), // 2 -> 3
      new Step(8_000_000L, 650, 7_500), // 3 -> 4
      new Step(15_000_000L, 600, 15_000), // 4 -> 5
      new Step(30_000_000L, 500, 30_000), // 5 -> 6
      new Step(50_000_000L, 400, 50_000), // 6 -> 7
      new Step(80_000_000L, 350, 80_000), // 7 -> 8
      new Step(120_000_000L, 250, 120_000), // 8 -> 9
      new Step(200_000_000L, 150, 200_000), // 9 -> 10
   };

   private UpgradeRules() {
   }

   /** Bậc nâng từ cấp {@code level}; null nếu đã tối đa. */
   public static Step step(int level) {
      return level >= 0 && level < STEPS.length ? STEPS[level] : null;
   }

   /** Tỉ lệ thành công (phần nghìn) khi bỏ {@code stones} đá ở bậc {@code step}. */
   public static int chancePermille(Step step, long stones) {
      if (step == null || stones <= 0) {
         return 0;
      }
      return (int) Math.min(step.ratePermille(), step.ratePermille() * Math.min(stones, step.stones()) / step.stones());
   }

   /** Trang bị nâng được (loại mặc được, ô 0..7). */
   public static boolean upgradable(ItemData item) {
      ItemPack.ItemType type = item == null ? null : item.type();
      return type != null && type.equippable() && type.equipSlot() >= 0 && type.equipSlot() <= 7
            && ((type.id() >= 3 && type.id() <= 10) || (type.id() >= 40 && type.id() <= 47));
   }

   /** Hệ của đá (Nhật / Nguyệt), {@link #GRADE_NONE} nếu không phải đá nâng cấp. */
   public static byte gradeOfStone(int templateId) {
      return templateId == SUN_STONE ? GRADE_SUN : templateId == MOON_STONE ? GRADE_MOON : GRADE_NONE;
   }

   public static int stoneOfGrade(byte grade) {
      return grade == GRADE_SUN ? SUN_STONE : grade == GRADE_MOON ? MOON_STONE : 0;
   }

   /** Món đặt được vào ô bảo hiểm: 3 loại bùa bảo hiểm + Ngọc trai đen. */
   public static boolean isInsurance(int templateId) {
      return templateId == INSURANCE_1 || templateId == INSURANCE_2 || templateId == INSURANCE_SPIRIT
            || templateId == BLACK_PEARL;
   }

   /** % cộng của cấp {@code level} (1..starCount) của món. */
   public static int percentOfLevel(ItemData item, int level) {
      byte[] up = item.upgradePercents;
      return up != null && level >= 1 && level <= up.length ? up[level - 1] : PERCENT_PER_LEVEL;
   }

   /** Tổng % cộng vào chỉ số gốc ở cấp hiện tại (đồ cũ: 4 × cấp). */
   public static int totalPercent(ItemData item) {
      int total = 0;
      for (int lv = 1; lv <= item.starCount; lv++) {
         total += percentOfLevel(item, lv);
      }
      return total;
   }

   /** Lên 1 cấp với {@code percent}% (ghi % từng cấp; mọi cấp 4% thì bỏ mảng). */
   public static void levelUp(ItemData item, int percent) {
      int next = item.starCount + 1;
      byte[] up = new byte[next];
      for (int lv = 1; lv < next; lv++) {
         up[lv - 1] = (byte) percentOfLevel(item, lv);
      }
      up[next - 1] = (byte) percent;
      item.starCount = (byte) next;
      item.upgradePercents = compact(up);
   }

   /** Về cấp {@code level} (≤ cấp hiện tại): bỏ % của các cấp trên. */
   public static void setLevel(ItemData item, int level) {
      level = Math.max(0, Math.min(level, item.starCount));
      byte[] up = new byte[level];
      for (int lv = 1; lv <= level; lv++) {
         up[lv - 1] = (byte) percentOfLevel(item, lv);
      }
      item.starCount = (byte) level;
      item.upgradePercents = compact(up);
   }

   private static byte[] compact(byte[] up) {
      for (byte p : up) {
         if (p != PERCENT_PER_LEVEL) {
            return up;
         }
      }
      return null;
   }

   /** Bộ id stat_type dạng % (dựng lại khi dữ liệu item nạp lại). */
   private record PercentIds(ItemPack pack, java.util.Set<Integer> ids) {
   }

   private static volatile PercentIds percentIds;

   /** Chỉ số dạng % (định dạng stat_type có "%") — không tăng theo cấp. */
   public static boolean isPercentStat(int statTypeId) {
      ItemPack pack = GameDataCache.item();
      PercentIds cached = percentIds;
      if (cached == null || cached.pack() != pack) {
         java.util.Set<Integer> ids = new java.util.HashSet<>();
         if (pack != null) {
            for (ItemPack.StatType t : pack.statTypes()) {
               if (t.formatPattern() != null && t.formatPattern().contains("%")) {
                  ids.add(t.id());
               }
            }
         }
         cached = new PercentIds(pack, java.util.Set.copyOf(ids));
         percentIds = cached;
      }
      return cached.ids().contains(statTypeId);
   }

   /** Giá trị 1 dòng chỉ số ở cấp {@code level} khi mọi cấp 4%: gốc × (100 + 4 × cấp) / 100, chỉ số % giữ nguyên. */
   public static int scaled(int statTypeId, int value, int level) {
      return scaledPercent(statTypeId, value, PERCENT_PER_LEVEL * Math.max(0, level));
   }

   /** Giá trị 1 dòng chỉ số của {@code item} theo % từng cấp đã nâng ({@link #totalPercent}), chỉ số % giữ nguyên. */
   public static int scaled(int statTypeId, int value, ItemData item) {
      return scaledPercent(statTypeId, value, item.starCount <= 0 ? 0 : totalPercent(item));
   }

   private static int scaledPercent(int statTypeId, int value, int percent) {
      if (percent <= 0 || isPercentStat(statTypeId)) {
         return value;
      }
      return (int) Math.min(Integer.MAX_VALUE, (long) value * (100 + percent) / 100);
   }
}
