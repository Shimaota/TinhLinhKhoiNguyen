package com.tlnt.server.combat;

/**
 * Công thức các chỉ số ở bảng thông tin nhân vật (spec người dùng 2026-10-10). Các chỉ số "điểm" dùng THANG 10.000:
 * 1.000 điểm = 10%.
 *
 * <pre>
 * Giáp / Xuyên giáp      giảm = (Giáp − Xuyên giáp) / 10000, tối đa 75%  — MỌI nơi: người ↔ người, quái ↔ người, boss
 * Né tránh / Chính xác   tỉ lệ né = (Né − Chính xác) / 10000, tối đa 75% — người né đòn người / quái / boss
 * Chí mạng               tỉ lệ = Chí mạng / 10000; sát thương × (100 + ST chí mạng% − Kháng ST chí mạng% của người bị đánh)
 * Khuếch đại tấn công    +% sát thương CHỈ đòn đánh thường (skill 1/2/8/14/20/26) — CombatService
 * Hấp thụ sát thương     nhận ĐỦ sát thương rồi hồi X% sát thương đó vào HP và X% vào MP (tối đa {@value #MAX_ABSORB_PERCENT}%)
 * Kháng sát thương       trừ thẳng X sát thương mỗi đòn nhận vào từ mọi nguồn (còn tối thiểu 1)
 * Phản sát thương        X% sát thương thật nhận vào dội lại người / quái / boss đánh mình
 * Choáng / Suy yếu / Băng giá / Thiêu đốt / Sấm sét  tỉ lệ = điểm / 10000 mỗi đòn trúng:
 *   choáng, băng, suy yếu: người + boss người chơi giả 3 s, quái (cả boss dạng quái) 5 s
 *   suy yếu: chậm 20% + nhận thêm 20% sát thương mọi nguồn
 *   thiêu đốt: mỗi giây 10% Tấn công người đốt; quái thường 5 s, boss + người 3 s
 *   sấm sét: thêm 1 đòn bằng 1 đòn đánh thường (100% Tấn công, qua giáp, không chí mạng)
 * Kháng hiệu ứng         giảm X% thời gian hiệu ứng xấu lên người
 * </pre>
 * Hiệu ứng đang dính thì không dính lại (không gia hạn) — tránh khống chế liên tục.
 */
public final class StatRules {

   /** Thang điểm: 10.000 = 100%. */
   public static final int SCALE = 10_000;
   /** Giảm sát thương tối đa do Giáp (phần vạn). */
   public static final int MAX_ARMOR = 7_500;
   /** Tỉ lệ né tối đa (phần vạn). */
   public static final int MAX_DODGE = 7_500;
   public static final int MAX_ABSORB_PERCENT = 80;

   public static final int STATUS_MS_PLAYER = 3_000;
   public static final int STATUS_MS_MOB = 5_000;
   public static final int WEAKEN_TAKEN_PERCENT = 20;
   /** Người chơi bị Suy yếu (option_info 6): chậm 50% + nhận thêm {@link #WEAKEN_TAKEN_PERCENT}%. */
   public static final int WEAKEN_SLOW_PERCENT = 50;
   /** Người chơi bị Thiêu đốt (option_info 7): mất 0,5% HP tối đa mỗi giây (phần nghìn). */
   public static final int BURN_PLAYER_PERMILLE = 5;
   public static final int BURN_ATTACK_PERCENT = 10;
   public static final int BURN_TICK_MS = 1_000;
   public static final int BURN_MS_MOB = 5_000;
   public static final int BURN_MS_BOSS_OR_PLAYER = 3_000;

   private StatRules() {
   }

   /** Phần vạn giảm do giáp: (Giáp − Xuyên giáp) kẹp [0, {@link #MAX_ARMOR}]. */
   public static long armorPermyriad(long armor, long armorPen) {
      return Math.min(MAX_ARMOR, Math.max(0, armor - Math.max(0, armorPen)));
   }

   /** Hệ số còn lại sau giáp (1 = không giảm). */
   public static double armorFactor(long armor, long armorPen) {
      return (SCALE - armorPermyriad(armor, armorPen)) / (double) SCALE;
   }

   /** Sát thương sau giáp; tối thiểu 1 nếu có sát thương. */
   public static long afterArmor(long damage, long armor, long armorPen) {
      if (damage <= 0) {
         return 0;
      }
      return Math.max(1, damage * (SCALE - armorPermyriad(armor, armorPen)) / SCALE);
   }

   /** Phần vạn né: (Né − Chính xác) kẹp [0, {@link #MAX_DODGE}]. */
   public static long dodgePermyriad(long evasion, long accuracy) {
      return Math.min(MAX_DODGE, Math.max(0, evasion - Math.max(0, accuracy)));
   }

   /** Né được với số ngẫu nhiên {@code roll} 0..9999. */
   public static boolean dodged(long evasion, long accuracy, int roll) {
      return roll < dodgePermyriad(evasion, accuracy);
   }

   /** % cộng thêm khi chí mạng sau Kháng sát thương chí mạng của người bị đánh (không âm). */
   public static long critBonus(long critDamagePercent, long critResistPercent) {
      return Math.max(0, critDamagePercent - Math.max(0, critResistPercent));
   }

   /** Phần HP / MP hồi lại từ {@code taken} sát thương vừa nhận (Hấp thụ sát thương). */
   public static long absorbed(long taken, long absorbPercent) {
      long p = Math.min(MAX_ABSORB_PERCENT, Math.max(0, absorbPercent));
      return taken <= 0 ? 0 : taken * p / 100;
   }

   /** Trừ Kháng sát thương (số thẳng), còn tối thiểu 1. */
   public static long afterResist(long damage, long damageResist) {
      if (damage <= 0) {
         return 0;
      }
      return Math.max(1, damage - Math.max(0, damageResist));
   }

   /** Sát thương dội lại (Phản sát thương %). */
   public static long reflected(long taken, long reflectPercent) {
      long p = Math.min(100, Math.max(0, reflectPercent));
      return taken <= 0 ? 0 : taken * p / 100;
   }

   /** Hiệu ứng kích hoạt với số ngẫu nhiên {@code roll} 0..9999 (điểm / 10000). */
   public static boolean procs(long points, int roll) {
      return points > 0 && roll < points;
   }

   /** Thời gian hiệu ứng sau Kháng hiệu ứng (%). */
   public static int afterEffectResist(int ms, long effectResistPercent) {
      long p = Math.min(100, Math.max(0, effectResistPercent));
      return (int) (ms * (100 - p) / 100);
   }

   /** Thiêu đốt lên người chơi: HP mất mỗi giây = 0,5% HP tối đa (tối thiểu 1). */
   public static long burnPlayerPerTick(long maxHp) {
      return Math.max(1, maxHp * BURN_PLAYER_PERMILLE / 1000);
   }

   /** Suy yếu: nhận thêm {@link #WEAKEN_TAKEN_PERCENT}%. */
   public static long weakened(long damage) {
      return damage + damage * WEAKEN_TAKEN_PERCENT / 100;
   }

   /** Thiêu đốt mỗi giây = 10% Tấn công người đốt (tối thiểu 1). */
   public static long burnPerTick(long attackPower) {
      return Math.max(1, attackPower * BURN_ATTACK_PERCENT / 100);
   }
}
