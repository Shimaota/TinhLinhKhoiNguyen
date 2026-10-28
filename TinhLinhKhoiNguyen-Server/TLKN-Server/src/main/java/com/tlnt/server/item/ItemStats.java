package com.tlnt.server.item;

import com.tlnt.server.forge.SetTitles;
import com.tlnt.server.forge.UpgradeRules;

import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.tribe.TribeRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cộng dòng chỉ số ({@link ItemData#stats}, id theo bảng {@code stat_type}) vào {@link PlayerStats}:
 * đồ mặc (10 ô), đeo lưng, và TOÀN BỘ món trong 4 bộ sưu tập (ngoại trang / danh hiệu / mặt nạ / thú cưng — vd có 10
 * ngoại trang thì cộng cả 10; món đang dùng chỉ để hiện ngoại hình, ẩn hay không vẫn tính).
 *
 * Cặp "+X" / "+X%" (vd 3 = +HP, 4 = +%HP): cuối = (gốc + tổng X) × (100 + tổng %) / 100. Chỉ số vốn đã là % (chí
 * mạng %, hút máu %...) thì cộng thẳng. Giá trị lấy {@code values[0]}. Dòng không ảnh hưởng chỉ số (HSD, hiệu ứng
 * đặc biệt...) bỏ qua.
 */
public final class ItemStats {

   private static final int MAX_STAT_ID = 128;
   /**
    * Thịt heo "Tăng X% mọi chỉ số" (đo game gốc mapdump 2026-10-06, Bruk): chỉ số THƯỜNG × (100 + X)% — HP, MP, Tấn công,
    * Tấn công quái, Chí mạng, Giáp, Xuyên giáp, Né, Chính xác, Tốc chạy, Hồi HP, Hồi MP (dòng "+X%" của các chỉ số này).
    */
   private static final int[] ALL_PERCENT_STATS = {4, 6, 8, 10, 12, 16, 18, 20, 22, 24, 26, 28};
   /**
    * ...còn chỉ số VỐN LÀ % thì CỘNG THẲNG +X: Sát thương chí mạng (80 -> 85), Hút HP / MP, Hút HP / MP quái, Phản đòn,
    * Khuếch đại, Hấp thụ, Kháng hiệu ứng, Thưởng kinh nghiệm. KHÔNG có: Kháng chí mạng, Kháng ST, Thưởng vàng, ST hiệu ứng.
    */
   private static final int[] ALL_FLAT_PERCENT_STATS = {14, 30, 32, 54, 56, 34, 44, 45, 46, 49};

   private ItemStats() {
   }

   /** Như {@link #apply(PlayerStats, PlayerData, PlayerStats)} khi không tách gốc (không có % nghề). */
   public static void apply(PlayerStats s, PlayerData d) {
      apply(s, d, null);
   }

   /**
    * Cộng mọi nguồn vào {@code s} (đang = gốc nhân vật + tiềm năng). 2 tầng, mỗi bước làm tròn xuống — khớp số đo game
    * gốc (mapdump 2026-10-06: Bruk HP 5524 / Giáp 290, Lyria Tấn công 1182):
    * <pre>
    * tầng 1 = floor(floor((gốc + tiềm năng + đồ+ + BST+ + bộ ấn+) × (100 + nghề%) / 100) × Π(1 + dòng% / 100))
    *          (mỗi dòng % của đồ nhân riêng; bộ sưu tập / bộ ấn: tổng % của cả bộ nhân 1 lần)
    * tầng 2 = floor((tầng 1 + buff+) × (100 + buff%) / 100)       buff = item loại 11 / phụ kiện kích hoạt
    * </pre>
    * nghề% ({@link Gender#bonusPercent}): Rinn +10% MP, Lyria +5% Tấn công, Bruk +10% HP / +5% Giáp — nhân lên cả gốc
    * {@code char_base_stat} (gốc là số THÔ: Bruk 2500 HP), KHÔNG nhân buff item loại 11 (Mật ong +100 Giáp = đúng +100).
    * Nấm hương: + 1% HP / MP tối đa (sau cùng) vào dòng Hồi HP / MP mỗi 5 s.
    *
    * @param base null = không có % nghề (test / chỉ số không theo nhân vật)
    */
   public static void apply(PlayerStats s, PlayerData d, PlayerStats base) {
      long now = System.currentTimeMillis();
      // Tầng 1: đồ, bộ sưu tập (cả đeo lưng), bộ ấn, Trái tim đại dương. Dòng "+X" cộng dồn vào sum; dòng "+X%" NHÂN NHAU từng
      // dòng vào mul (game gốc: Tốc chạy 450 × 1,04 giày × 1,09 roi = 510,1 -> 510; cộng dồn 13% chỉ ra 508).
      long[] sum = new long[MAX_STAT_ID];
      double[] mul = new double[MAX_STAT_ID];
      java.util.Arrays.fill(mul, 1.0);
      for (ItemData item : d.body.asList()) {
         addLines(sum, mul, item);
      }
      for (CollectionKind kind : CollectionKind.values()) { // bộ sưu tập: tổng của cả bộ = 1 nguồn (như "Chỉ số cộng thêm")
         long[] kindSum = new long[MAX_STAT_ID];
         for (ItemData item : kind.of(d).items) {
            add(kindSum, item);
         }
         merge(sum, mul, kindSum);
      }
      long[] titles = new long[MAX_STAT_ID];
      SetTitles.addStats(titles, SetTitles.compute(d)); // bộ Nhật ấn / Nguyệt ấn (8 món cùng hệ)
      merge(sum, mul, titles);
      long[] tribe = new long[MAX_STAT_ID]; // bộ lạc: kỹ năng tĩnh + biểu tượng + "đứng cạnh" = 1 nguồn
      TribeRules.addStats(tribe, d);
      merge(sum, mul, tribe);
      if (d.nearWater && d.gear != null && d.gear.oceanHeart) { // Trái tim đại dương: đứng gần nước
         sum[23] += GearCharges.OCEAN_MOVE_SPEED;
         mul[8] *= 1 + GearCharges.OCEAN_ATTACK_PERCENT / 100.0;
      }
      long[] buff = new long[MAX_STAT_ID]; // tầng 2: hiệu ứng item loại 11 + phụ kiện kích hoạt
      ItemBuffs.addStats(buff, d, now);
      long all = buff[ItemBuffs.STAT_ALL_PERCENT]; // Thịt heo: +% mọi chỉ số
      if (all != 0) {
         for (int id : ALL_PERCENT_STATS) {
            buff[id] += all; // × (100 + X)%
         }
         for (int id : ALL_FLAT_PERCENT_STATS) {
            buff[id] += all; // chỉ số % : + X
         }
      }
      int gender = base == null ? 0 : d.gender;

      s.maxHp = stat(s.maxHp, sum, mul, buff, 3, 4, Gender.bonusPercent(gender, 3));
      s.maxMp = stat(s.maxMp, sum, mul, buff, 5, 6, Gender.bonusPercent(gender, 5));
      s.attackPower = stat(s.attackPower, sum, mul, buff, 7, 8, Gender.bonusPercent(gender, 7));
      s.attackVsMob = two(s.attackVsMob, sum[9], mul[10] * mul[13], buff[9], buff[10] + buff[13]);
      s.critChance = two(s.critChance, sum[11], mul[12], buff[11], buff[12]);
      s.armor = stat(s.armor, sum, mul, buff, 15, 16, Gender.bonusPercent(gender, 15));
      s.armorPen = two(s.armorPen, sum[17], mul[18], buff[17], buff[18]);
      s.evasion = two(s.evasion, sum[19], mul[20], buff[19], buff[20]);
      s.accuracy = two(s.accuracy, sum[21], mul[22], buff[21], buff[22]);
      s.moveSpeed = two(s.moveSpeed, sum[23], mul[24], buff[23], buff[24]);
      s.hpRegen = two(s.hpRegen, sum[25], mul[26], buff[25], buff[26]);
      s.mpRegen = two(s.mpRegen, sum[27], mul[28], buff[27], buff[28]);
      s.stunDamage = two(s.stunDamage, sum[35], mul[36], buff[35], buff[36]);
      s.weakenDamage = two(s.weakenDamage, sum[37], mul[38], buff[37], buff[38]);
      s.iceDamage = two(s.iceDamage, sum[39], mul[40], buff[39], buff[40]);
      s.thunderDamage = two(s.thunderDamage, sum[41], mul[42], buff[41], buff[42]);
      s.burnDamage = two(s.burnDamage, sum[50], mul[51], buff[50], buff[51]);
      for (int i = 0; i < MAX_STAT_ID; i++) { // chỉ số vốn là % / cộng thẳng: 2 tầng như nhau
         sum[i] += buff[i];
      }
      s.critDamagePercent += sum[14];
      s.hpLeechPercent += sum[29] + sum[30];
      s.mpLeechPercent += sum[31] + sum[32];
      s.reflectDamagePercent += sum[33] + sum[34];
      s.critResistPercent += sum[43];
      s.damageAmpPercent += sum[44];
      s.damageAbsorbPercent += sum[45];
      s.effectResistPercent += sum[46];
      s.freezeReducePercent += sum[71];
      s.coldResist = sum[STAT_COLD_RESIST] > 0;
      s.damageResist += sum[47];
      s.goldBonusPercent += sum[48];
      s.expBonusPercent += sum[49];
      s.hpLeechVsMobPercent += sum[53] + sum[54];
      s.mpLeechVsMobPercent += sum[56];
      s.mobDamagePercent += sum[ItemBuffs.STAT_MOB_DAMAGE];
      s.mobDamageTakenReducePercent += sum[ItemBuffs.STAT_MOB_TAKEN];
      int[] regen = ItemBuffs.regenPercents(d, now); // Nấm hương: % HP / MP tối đa vào dòng Hồi mỗi 5 s
      int tribeRegen = TribeRules.regenPercent(d); // kỹ năng bộ lạc 8 Dòng Chảy Sinh Mệnh
      s.hpRegen += s.maxHp * (regen[0] + tribeRegen) / 100;
      s.mpRegen += s.maxMp * (regen[1] + tribeRegen) / 100;
   }

   /** Chỉ số có % nghề: {@code current} = gốc + tiềm năng; tầng 1 rồi tầng 2, mỗi bước làm tròn xuống. */
   static long stat(long current, long[] sum, double[] mul, long[] buff, int flat, int percent, int classPercent) {
      long withClass = Math.floorDiv((current + sum[flat]) * (100 + classPercent), 100);
      long stage1 = (long) Math.floor(withClass * mul[percent] + 1e-9);
      return Math.floorDiv((stage1 + buff[flat]) * (100 + buff[percent]), 100); // buff loại 11: cộng thẳng
   }

   /**
    * Chỉ số không có % nghề: tầng 1 = (gốc + đồ+) × tích các dòng % của đồ / bộ sưu tập, tầng 2 = (tầng 1 + buff+) × (100 +
    * buff%), mỗi tầng làm tròn xuống.
    */
   private static long two(long base, long flat1, double mul1, long flat2, long percent2) {
      return scale((long) Math.floor((base + flat1) * mul1 + 1e-9), flat2, percent2);
   }


   /**
    * Gộp dòng chỉ số của nhiều item theo {@code statTypeId} (cộng từng giá trị), giữ thứ tự xuất hiện đầu tiên —
    * dùng cho "Chỉ số cộng thêm" của bộ sưu tập (op 48/50/64/66 sub 0).
    */
   public static List<ItemData.StatValue> sum(Iterable<ItemData> items) {
      Map<Integer, int[]> merged = new LinkedHashMap<>();
      for (ItemData item : items) {
         for (ItemData.StatValue stat : item.stats) {
            int[] values = stat.values() == null ? new int[0] : stat.values();
            int[] total = merged.get(stat.statTypeId());
            if (total == null) {
               merged.put(stat.statTypeId(), values.clone());
            } else {
               if (total.length < values.length) {
                  total = Arrays.copyOf(total, values.length);
                  merged.put(stat.statTypeId(), total);
               }
               for (int k = 0; k < values.length; k++) {
                  total[k] += values[k];
               }
            }
         }
      }
      List<ItemData.StatValue> result = new ArrayList<>(merged.size());
      merged.forEach((id, values) -> result.add(new ItemData.StatValue(id, values)));
      return result;
   }

   /** stat_type "+X%" đi cặp với "+X" (HP, MP, Tấn công, Tấn công quái, Chí mạng, Giáp, ...): NHÂN nhau theo từng dòng. */
   private static final boolean[] PAIR_PERCENT = new boolean[MAX_STAT_ID];

   static {
      for (int id : new int[] {4, 6, 8, 10, 12, 13, 16, 18, 20, 22, 24, 26, 28, 36, 38, 40, 42, 51}) {
         PAIR_PERCENT[id] = true;
      }
   }

   /** Dòng 70 "Chống lạnh" (không có số). */
   static final int STAT_COLD_RESIST = 70;
   /** Dòng bật / tắt (mẫu chữ không có {0}): có dòng = 1, bất kể giá trị lưu. 70 Chống lạnh, 72 Không giới hạn Táo. */
   private static final boolean[] FLAG_LINE = new boolean[MAX_STAT_ID];

   static {
      FLAG_LINE[STAT_COLD_RESIST] = true;
      FLAG_LINE[72] = true;
   }

   /** 1 món (+ bùa khảm): dòng "+X" cộng vào {@code sum}, mỗi dòng "+X%" nhân vào {@code mul}. */
   private static void addLines(long[] sum, double[] mul, ItemData item) {
      if (item == null) {
         return;
      }
      long[] one = new long[MAX_STAT_ID];
      for (ItemData.StatValue stat : item.stats) {
         int id = stat.statTypeId();
         if (id > 0 && id < MAX_STAT_ID && stat.values() != null && stat.values().length > 0) {
            long v = FLAG_LINE[id] ? 1 : UpgradeRules.scaled(id, stat.values()[0], item);
            if (PAIR_PERCENT[id]) {
               mul[id] *= 1 + v / 100.0;
            } else {
               one[id] += v;
            }
         }
      }
      for (ItemData charm : item.soulCharms) {
         if (charm != null) {
            addLines(one, mul, charm);
         }
      }
      addElements(one, item);
      for (int i = 0; i < MAX_STAT_ID; i++) {
         sum[i] += one[i];
      }
   }

   /** 1 nguồn đã cộng gộp (cả bộ sưu tập / bộ ấn): "+X" cộng, tổng "+X%" của nguồn nhân 1 lần. */
   private static void merge(long[] sum, double[] mul, long[] source) {
      for (int i = 0; i < MAX_STAT_ID; i++) {
         if (source[i] == 0) {
            continue;
         }
         if (PAIR_PERCENT[i]) {
            mul[i] *= 1 + source[i] / 100.0;
         } else {
            sum[i] += source[i];
         }
      }
   }

   private static void add(long[] sum, ItemData item) {
      if (item == null) {
         return;
      }
      for (ItemData.StatValue stat : item.stats) {
         int id = stat.statTypeId();
         if (id > 0 && id < MAX_STAT_ID && stat.values() != null && stat.values().length > 0) {
            sum[id] += FLAG_LINE[id] ? 1 // dòng không có số: có là có
                  : UpgradeRules.scaled(id, stat.values()[0], item); // +4% (Ngọc trai đen 12%) / cấp (trừ chỉ số %)
         }
      }
      for (ItemData charm : item.soulCharms) { // bùa linh hồn đã khảm: cộng nguyên chỉ số (không theo cấp nâng)
         if (charm == null) {
            continue;
         }
         for (ItemData.StatValue stat : charm.stats) {
            int id = stat.statTypeId();
            if (id > 0 && id < MAX_STAT_ID && stat.values() != null && stat.values().length > 0) {
               sum[id] += stat.values()[0];
            }
         }
      }
      addElements(sum, item);
   }

   /** Đá nguyên tố đã khảm ({@code forge/ElementRules}): cộng thẳng (không theo cấp nâng, không phải dòng "+X%" cặp). */
   private static void addElements(long[] sum, ItemData item) {
      for (ItemData.ElementStone stone : item.elementStones) {
         if (stone != null && stone.statTypeId() > 0 && stone.statTypeId() < MAX_STAT_ID && stone.values() != null
               && stone.values().length > 0) {
            sum[stone.statTypeId()] += stone.values()[0];
         }
      }
   }

   private static long scale(long base, long flat, long percent) {
      return (base + flat) * (100 + percent) / 100;
   }
}
