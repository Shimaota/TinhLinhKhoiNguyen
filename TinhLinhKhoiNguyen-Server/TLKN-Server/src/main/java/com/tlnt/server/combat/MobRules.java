package com.tlnt.server.combat;

import java.util.Map;

/**
 * Công thức chiến đấu với quái + EXP, ĐO TỪ GAME GỐC (agent mapdump nghe gói client game gốc, ~1.900 đòn / lần EXP,
 * nhân vật cấp 18). Quái CÙNG CẤP + CÙNG BIẾN THỂ thì mạnh như nhau — không có hệ số riêng theo mob_template (game gốc
 * có vài ngoại lệ nhỏ như Tê giác ×1,23, cố ý bỏ cho dễ cân bằng).
 *
 * <pre>
 * Người đánh quái (sau khi CombatService đã tính thô = (Tấn công + Tấn công lên quái) × % kỹ năng ± 5%):
 *   cấp người − cấp quái ≥ 10 : (như dưới) × 20%                  (dump 2026-10-06: giáp quái vẫn tính)
 *   ngược lại                 : thô × (1 − min(75%, (giáp quái − Xuyên giáp) / 10000))   ({@link StatRules})
 *   giáp quái = 21,8 × cấp + 50
 *
 * Quái đánh người (mỗi {@link #MOB_ATTACK_INTERVAL_MS} ms):
 *   17 × (cấp + 1) × biến thể (Thường 1 / Tinh anh 3 / Thủ lĩnh 5) × (cấp ≥ 30 ? 2 : 1)
 *     × (1 − min(75%, Giáp người / 10000)) ± 5%, tối thiểu 1, không quá HP còn.
 *
 * EXP mỗi đòn (sát thương THẬT gây ra — đã cắt theo HP quái còn; công thức spec 2026-10-12, thay bảng đo cũ):
 *   d   = cấp người − cấp quái
 *   P   = 1                                      nếu d ≤ 0
 *         max(exp.penaltyMin, 1 − exp.penaltyStep × d)   nếu 1 ≤ d ≤ 9
 *         map thường (ST đã cắt còn 20%): 1 / Tế đàn (ST đủ): exp.penaltyMin   nếu d ≥ 10
 *   gốc = max(1, floor(sát thương × exp.field × cấp quái × P × biến thể (1 / 2 / 3; = 1 khi d ≥ 10)))
 *   EXP = floor(gốc × (1 + EXP% / 100)) × {@code exp.rate}
 * Rừng Linh Mộc dùng hệ số riêng {@link #dungeonExpPerDamage} = exp.dungeon × cấp lượt ({@code linhmoc/LinhMocRules}).
 * </pre>
 *
 * Độ khớp với game gốc: sát thương người -> quái (đo với công thức giáp cũ 2200/(2200+Giáp) — đổi sang thang /10000 theo spec 2026-10-10) 95% số đòn trong ±5%; quái -> người 100% trong ±10%.
 */
public final class MobRules {

   /** Hằng số giáp CŨ (đo từ game gốc) — không còn dùng tính sát thương, giữ để đối chiếu số đo. */
   public static final double ARMOR_K = 2200;
   /** Giáp quái theo cấp. */
   public static final double MOB_ARMOR_PER_LEVEL = 21.8;
   public static final double MOB_ARMOR_BASE = 50;
   /**
    * Người cao hơn quái từ bấy nhiêu cấp: sát thương lên quái chỉ còn {@link #LOW_LEVEL_EXP_DAMAGE_PERCENT}% (sau giáp) —
    * như game gốc (mapdump 2026-10-06, Lyria cấp 18: Bò lv3 / Tê giác lv5 / Heo chiến binh lv7 = 0,19-0,22 × sát thương
    * đủ, Báo rừng lv12 (chênh 6) trở lên = đủ). Bỏ 1 lần sáng 2026-10-06, bật lại cùng ngày để khớp game gốc.
    */
   public static final int LOW_LEVEL_GAP = 10;
   public static final int LOW_LEVEL_EXP_DAMAGE_PERCENT = 20;

   /** Sát thương gốc quái = hằng số × (cấp + 1). */
   public static final int MOB_DAMAGE_PER_LEVEL = 17;
   /** Quái từ cấp này đánh ×{@link #HIGH_LEVEL_MULTIPLIER} (game gốc: lv25 chưa, lv30 đã — ngưỡng trong 26..30). */
   public static final int HIGH_LEVEL_FROM = 30;
   public static final int HIGH_LEVEL_MULTIPLIER = 2;
   /** Biến thể ({@code mob_variant.idx}). */
   public static final int VARIANT_NORMAL = 0;
   public static final int VARIANT_ELITE = 1;
   public static final int VARIANT_BOSS = 2;
   /**
    * Quái thường bị hạ, lần hồi sinh kế thành Tinh anh / Thủ lĩnh theo TRỌNG SỐ: thường 97 : tinh anh 2 : thủ lĩnh 1 (2% /
    * 1%), chỉ loại quái có đủ biến thể trong mob_variant; mỗi khu, MỖI LOẠI QUÁI tối đa {@link #MAX_ELITE_PER_ZONE} tinh
    * anh + {@link #MAX_BOSS_PER_ZONE} thủ lĩnh còn sống (khu 3 loại quái -> tối đa 3 + 3) — {@code world/ZoneMobs}.
    */
   public static final int NORMAL_WEIGHT = 97;
   public static final int ELITE_WEIGHT = 2;
   public static final int BOSS_WEIGHT = 1;
   /** Số ngẫu nhiên của {@link #rollVariant}: 0 .. ROLL_RANGE - 1. */
   public static final int ROLL_RANGE = NORMAL_WEIGHT + ELITE_WEIGHT + BOSS_WEIGHT;
   public static final int MAX_ELITE_PER_ZONE = 1;
   public static final int MAX_BOSS_PER_ZONE = 1;
   /** HP theo biến thể (×HP quái thường) — đọc từ dữ liệu map game gốc: Tinh anh ×10, Thủ lĩnh ×35. */
   private static final int[] VARIANT_HP = {1, 10, 35};
   /** Sát thương quái theo biến thể ({@code mob_variant.idx}): Thường, Tinh anh, Thủ lĩnh. */
   private static final int[] VARIANT_ATTACK = {1, 3, 5};
   /** Dao động sát thương quái ±%. */
   public static final int MOB_DAMAGE_SPREAD_PERCENT = 5;
   /** Nhịp quái đánh (đo: đúng 3,5 s / đòn mỗi con). */
   public static final long MOB_ATTACK_INTERVAL_MS = 3500;
   /** Đòn đầu sau khi bị đánh (quái quay sang đánh trả). */
   public static final long MOB_FIRST_ATTACK_MS = 1000;

   /** EXP theo biến thể: Thường, Tinh anh, Thủ lĩnh. Vùng người cao hơn ≥ {@link #LOW_LEVEL_GAP} cấp: ×1. */
   private static final int[] VARIANT_EXP = {1, 2, 3};
   /** Sai số double trước floor (vd 1000 × 0,0018 × 10 có thể ra 17,99999…). */
   private static final double EPS = 1e-6;

   /** EXP gốc / 1 sát thương / 1 cấp quái ({@code exp.field}). */
   private static volatile double expField = 0.0018;
   /** Mỗi cấp người cao hơn quái (1..9) trừ bấy nhiêu hệ số EXP ({@code exp.penaltyStep}). */
   private static volatile double expPenaltyStep = 0.1;
   /** Hệ số EXP thấp nhất khi người cao hơn quái ({@code exp.penaltyMin}). */
   private static volatile double expPenaltyMin = 0.2;
   /** EXP / 1 sát thương / 1 cấp lượt ở Rừng Linh Mộc ({@code exp.dungeon}). */
   private static volatile double expDungeon = 0.02;

   /** Nhân EXP toàn server ({@code exp.rate}, 1 = gốc, 2 = x2). Đặt 1 lần lúc khởi động. */
   private static volatile int expRate = 1;
   /** Quái đánh trả người đánh nó ({@code mob.attack}). */
   private static volatile boolean mobAttack = true;
   /** Quái thường hồi sinh sau bấy nhiêu giây ({@code mob.respawnSeconds}); ≤ 0 = theo {@code respawn} của điểm sinh. */
   private static volatile int respawnSeconds = 5;

   private MobRules() {
   }

   public static void setExpRate(int rate) {
      expRate = Math.max(1, rate);
   }

   public static int expRate() {
      return expRate;
   }

   /** Đặt công thức EXP ({@code exp.field / exp.penaltyStep / exp.penaltyMin / exp.dungeon}) 1 lần lúc khởi động. */
   public static void setExpFormula(double field, double penaltyStep, double penaltyMin, double dungeon) {
      expField = Math.max(0, field);
      expPenaltyStep = Math.max(0, penaltyStep);
      expPenaltyMin = Math.max(0, Math.min(1, penaltyMin));
      expDungeon = Math.max(0, dungeon);
   }

   /** {exp.field, exp.penaltyStep, exp.penaltyMin, exp.dungeon} đang dùng (admin panel). */
   public static double[] expFormula() {
      return new double[] {expField, expPenaltyStep, expPenaltyMin, expDungeon};
   }

   public static int respawnSeconds() {
      return respawnSeconds;
   }

   /** EXP / 1 sát thương ở Rừng Linh Mộc với cấp lượt {@code level}: {@code exp.dungeon} × cấp. */
   public static double dungeonExpPerDamage(int level) {
      return expDungeon * Math.max(1, level);
   }

   public static void setMobAttack(boolean enabled) {
      mobAttack = enabled;
   }

   public static boolean mobAttack() {
      return mobAttack;
   }

   public static void setRespawnSeconds(int seconds) {
      respawnSeconds = seconds;
   }

   /** Thời gian hồi sinh (ms) của 1 con quái thường có {@code spawnRespawnSeconds} trong map_template. */
   public static long respawnMs(int spawnRespawnSeconds) {
      int seconds = respawnSeconds > 0 ? respawnSeconds : spawnRespawnSeconds;
      return Math.max(1, seconds) * 1000L;
   }

   // ------------------------------------------------------------------ người đánh quái

   public static long mobArmor(int mobLevel) {
      return Math.round(MOB_ARMOR_PER_LEVEL * Math.max(1, mobLevel) + MOB_ARMOR_BASE);
   }

   /**
    * Sát thương thật lên quái từ sát thương thô {@code raw} (đã có % kỹ năng + dao động): chỉ giảm theo giáp quái (trừ
    * Xuyên giáp); người cao hơn quái ≥ {@link #LOW_LEVEL_GAP} cấp thì chỉ còn {@link #LOW_LEVEL_EXP_DAMAGE_PERCENT}%.
    *
    * @param armorPen Xuyên giáp của người đánh
    */
   public static long playerHitsMob(long raw, int playerLevel, int mobLevel, long armorPen) {
      return playerHitsMob(raw, playerLevel, mobLevel, armorPen, true);
   }

   /** Như trên; {@code levelCut} = false (phó bản) thì KHÔNG giảm còn 20% khi người cao hơn quái ≥ 10 cấp. */
   public static long playerHitsMob(long raw, int playerLevel, int mobLevel, long armorPen, boolean levelCut) {
      if (raw <= 0) {
         return 0;
      }
      long armor = Math.max(0, mobArmor(mobLevel) - Math.max(0, armorPen));
      double damage = raw * StatRules.armorFactor(armor, 0);
      if (levelCut && playerLevel - mobLevel >= LOW_LEVEL_GAP) {
         damage = damage * LOW_LEVEL_EXP_DAMAGE_PERCENT / 100; // quái thấp hơn ≥ 10 cấp: chỉ 20% (trừ phó bản)
      }
      return Math.max(1, (long) damage);
   }

   // ------------------------------------------------------------------ quái đánh người

   /** Sát thương quái chưa dao động (để kiểm thử / hiển thị). */
   public static double mobBaseDamage(int mobLevel, int variant, long playerArmor) {
      int level = Math.max(1, mobLevel);
      double base = (double) MOB_DAMAGE_PER_LEVEL * (level + 1) * VARIANT_ATTACK[clampVariant(variant)];
      if (level >= HIGH_LEVEL_FROM) {
         base *= HIGH_LEVEL_MULTIPLIER;
      }
      return base * StatRules.armorFactor(playerArmor, 0);
   }

   /**
    * 1 đòn quái lên người.
    *
    * @param spreadPercent dao động -{@link #MOB_DAMAGE_SPREAD_PERCENT}..+{@link #MOB_DAMAGE_SPREAD_PERCENT}
    */
   public static long mobHitsPlayer(int mobLevel, int variant, long playerArmor, int spreadPercent) {
      double damage = mobBaseDamage(mobLevel, variant, playerArmor) * (100 + spreadPercent) / 100.0;
      return Math.max(1, Math.round(damage));
   }

   /**
    * 1 đòn của quái có sát thương GỐC cho sẵn theo bảng ({@code altar/AltarRules#baseDamage}): vẫn giảm theo Giáp
    * ×2200/(2200+Giáp) và dao động như quái thường.
    */
   public static long tableHitsPlayer(long baseDamage, long playerArmor, int spreadPercent) {
      double damage = baseDamage * StatRules.armorFactor(playerArmor, 0) * (100 + spreadPercent) / 100.0;
      return Math.max(1, Math.round(damage));
   }

   // ------------------------------------------------------------------ đồ rơi của Tinh anh / Thủ lĩnh

   /**
    * Đồ quý rơi khi hạ Tinh anh / Thủ lĩnh (người KHÔNG ở bước nhiệm vụ cần món đó — người đang làm nhiệm vụ thì 100% rơi
    * riêng, {@code QuestEvents.questDrop}): mob_template.id -> {item_template.id, % rơi Tinh anh, % rơi Thủ lĩnh}. 7 Hổ
    * răng kiếm -> 262 Nanh Hổ răng kiếm: Tinh anh 1%, Thủ lĩnh 5% (giảm 2026-10-06, trước 10% / 20%).
    */
   private static final Map<Integer, int[]> ELITE_DROPS = Map.of(7, new int[] {262, 1, 5});

   /** Món rơi khi hạ quái {@code mobId} biến thể {@code variant} với số ngẫu nhiên {@code roll} 0..99; 0 = không rơi. */
   public static int eliteDrop(int mobId, int variant, int roll) {
      int[] drop = variant == VARIANT_NORMAL ? null : ELITE_DROPS.get(mobId);
      if (drop == null) {
         return 0;
      }
      int percent = variant == VARIANT_BOSS ? drop[2] : drop[1];
      return roll < percent ? drop[0] : 0;
   }

   /** Người cao hơn siêu quái từ bấy nhiêu cấp: siêu quái không rơi đồ (spec 2026-10-11). */
   public static final int ELITE_NO_DROP_GAP = 15;
   /** Đá nguyên tố (127 Kim .. 133 Lôi). */
   static final int ELEMENT_STONE_FIRST = 127;
   static final int ELEMENT_STONE_LAST = 133;

   /**
    * Quà của siêu quái cho người kết liễu (rơi RIÊNG, khoá): Tinh anh 1-2 Sách tiềm năng 145, 10-20 Đá khởi nguyên 274, 1
    * Đá nguyên tố ngẫu nhiên; Thủ lĩnh 1-3 sách, 20-30 đá, 2 Đá nguyên tố ngẫu nhiên. Cao hơn quái ≥ {@link
    * #ELITE_NO_DROP_GAP} cấp -> không rơi gì (danh sách rỗng). Quái thường -> rỗng.
    */
   public static java.util.List<com.tlnt.server.player.ItemData> eliteLoot(int variant, int playerLevel, int mobLevel,
         java.util.Random r) {
      java.util.List<com.tlnt.server.player.ItemData> out = new java.util.ArrayList<>();
      if (variant == VARIANT_NORMAL || playerLevel - mobLevel >= ELITE_NO_DROP_GAP) {
         return out;
      }
      boolean leader = variant == VARIANT_BOSS;
      out.add(locked(145, leader ? 1 + r.nextInt(3) : 1 + r.nextInt(2)));
      out.add(locked(274, leader ? 20 + r.nextInt(11) : 10 + r.nextInt(11)));
      for (int k = 0; k < (leader ? 2 : 1); k++) {
         out.add(locked(ELEMENT_STONE_FIRST + r.nextInt(ELEMENT_STONE_LAST - ELEMENT_STONE_FIRST + 1), 1));
      }
      return out;
   }

   private static com.tlnt.server.player.ItemData locked(int templateId, int quantity) {
      com.tlnt.server.player.ItemData item = com.tlnt.server.player.ItemData.of(templateId, quantity);
      item.locked = true;
      return item;
   }

   // ------------------------------------------------------------------ EXP

   /**
    * Hệ số EXP P theo chênh d = cấp người − cấp quái: d ≤ 0 -> 1; 1..9 -> max(penaltyMin, 1 − penaltyStep × d); ≥ 10 ->
    * 1 nếu {@code levelCut} (sát thương đã bị cắt còn 20%), ngược lại (Tế đàn, sát thương đủ) penaltyMin.
    */
   public static double expLevelFactor(int playerLevel, int mobLevel, boolean levelCut) {
      int diff = playerLevel - mobLevel;
      if (diff <= 0) {
         return 1;
      }
      if (diff >= LOW_LEVEL_GAP) {
         return levelCut ? 1 : expPenaltyMin;
      }
      return Math.max(expPenaltyMin, 1 - expPenaltyStep * diff);
   }

   /**
    * EXP 1 đòn ở map thường.
    *
    * @param dealt           sát thương THẬT (đã cắt theo HP còn; quái 1 HP / đòn thì = 1)
    * @param expBonusPercent chỉ số "Thưởng kinh nghiệm" của người đánh (đồ, bộ sưu tập, tổ đội, thuốc)
    */
   public static long hitExp(long dealt, int playerLevel, int mobLevel, int variant, long expBonusPercent) {
      return hitExp(dealt, playerLevel, mobLevel, variant, expBonusPercent, true);
   }

   /**
    * Như trên; {@code levelCut} = false (Tế đàn: sát thương KHÔNG bị giảm 20%) thì người cao hơn quái ≥ 10 cấp nhận hệ
    * số {@code exp.penaltyMin} — cùng 1 đòn được EXP xấp xỉ map thường, không gấp 5.
    */
   public static long hitExp(long dealt, int playerLevel, int mobLevel, int variant, long expBonusPercent,
         boolean levelCut) {
      if (dealt <= 0) {
         return 0;
      }
      boolean lowLevel = playerLevel - mobLevel >= LOW_LEVEL_GAP;
      int variantFactor = lowLevel ? 1 : VARIANT_EXP[clampVariant(variant)];
      double raw = dealt * expField * Math.max(1, mobLevel) * expLevelFactor(playerLevel, mobLevel, levelCut)
            * variantFactor;
      long base = Math.max(1, (long) Math.floor(raw + EPS));
      long withBonus = (long) Math.floor(base * (1 + Math.max(0, expBonusPercent) / 100.0) + EPS);
      return Math.max(1, withBonus) * expRate;
   }

   // ------------------------------------------------------------------ nội bộ

   /** HP của biến thể {@code variant} khi quái thường có {@code normalHp}. */
   public static long variantHp(long normalHp, int variant) {
      return normalHp * VARIANT_HP[clampVariant(variant)];
   }

   /** Biến thể lúc hồi sinh theo số ngẫu nhiên {@code roll} 0 .. {@link #ROLL_RANGE} - 1 (trọng số thủ lĩnh / tinh anh / thường). */
   public static int rollVariant(int roll) {
      if (roll < BOSS_WEIGHT) {
         return VARIANT_BOSS;
      }
      return roll < BOSS_WEIGHT + ELITE_WEIGHT ? VARIANT_ELITE : VARIANT_NORMAL;
   }

   private static int clampVariant(int variant) {
      return Math.max(0, Math.min(VARIANT_ATTACK.length - 1, variant));
   }
}
