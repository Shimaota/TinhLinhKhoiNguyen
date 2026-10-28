package com.tlnt.server.player;

import com.tlnt.server.combat.MobRules;

/**
 * Cấp độ + EXP + điểm tiềm năng khi lên cấp. EXP cần mỗi cấp: {@link PlayerStats#expToNextLevel}.
 *
 * <p>Điểm tiềm năng nhận khi LÊN tới cấp {@code L} ({@link #talentPointsAt}): {@code 10 + (L - 2) × 11}
 * <pre>
 * 1 -> 2: 10   2 -> 3: 21   3 -> 4: 32   4 -> 5: 43   5 -> 6: 54   6 -> 7: 65 ...
 * </pre>
 *
 * <p>Nhiều luồng có thể sửa (luồng session, luồng tick khu khi PK) -> ghi trong {@code synchronized (d)} như vàng /
 * HP. Gọi xong thì tự gửi lại khối chỉ số ({@code World.broadcastStats}) — class này không phát gói.
 */
public final class Leveling {

   /** Cấp tối đa = cấp lớn nhất trong bảng {@code tlnt_data.level_exp} ({@link LevelTable}). */
   public static int maxLevel() {
      return LevelTable.maxLevel();
   }

   private Leveling() {
   }

   /**
    * EXP 1 đòn đánh quái ở map thường — {@link MobRules#hitExp}:
    * <pre>
    * gốc = max(1, floor(sát thương thật × exp.field × cấp quái × P × biến thể (Thường 1 / Tinh anh 2 / Thủ lĩnh 3)))
    * EXP = floor(gốc × (1 + Thưởng kinh nghiệm % / 100)) × exp.rate
    * </pre>
    * P theo d = cấp người − cấp quái: d ≤ 0 -> 1; 1..9 -> max(exp.penaltyMin, 1 − exp.penaltyStep × d); ≥ 10 -> 1 (sát
    * thương đã bị giảm còn 20%, biến thể ×1). Không bao giờ về 0 (gốc tối thiểu 1).
    *
    * @param dealt           sát thương THẬT (đã cắt theo HP còn — đòn kết liễu không bị lố)
    * @param expBonusPercent chỉ số "Thưởng kinh nghiệm" ({@link PlayerStats#expBonusPercent})
    */
   public static long mobHitExp(long dealt, int playerLevel, int mobLevel, int mobVariant, long expBonusPercent) {
      return MobRules.hitExp(dealt, playerLevel, mobLevel, mobVariant, expBonusPercent);
   }

   /**
    * Như trên; {@code levelCut} = false ở Tế đàn (sát thương không cắt 20%): cao hơn quái ≥ 10 cấp thì P = exp.penaltyMin
    * ({@link MobRules#hitExp(long, int, int, int, long, boolean)}).
    */
   public static long mobHitExp(long dealt, int playerLevel, int mobLevel, int mobVariant, long expBonusPercent,
         boolean levelCut) {
      return MobRules.hitExp(dealt, playerLevel, mobLevel, mobVariant, expBonusPercent, levelCut);
   }

   /** Điểm tiềm năng nhận khi vừa lên tới cấp {@code level} (≥ 2). */
   public static int talentPointsAt(int level) {
      return level < 2 ? 0 : 10 + (level - 2) * 11;
   }

   /**
    * Cộng EXP, đủ thì lên cấp (có thể nhiều cấp 1 lần), mỗi cấp cộng {@link #talentPointsAt} điểm tiềm năng. Cấp tối
    * đa thì EXP dừng ở mức cần cho cấp kế.
    *
    * @return số cấp vừa lên (0 = không lên cấp)
    */
   public static int addExp(PlayerData d, long amount) {
      if (amount <= 0) {
         return 0;
      }
      synchronized (d) {
         int gained = 0;
         long exp = d.exp + amount;
         int max = maxLevel();
         while (d.level < max) {
            long need = PlayerStats.expToNextLevel(d.level);
            if (exp < need) {
               break;
            }
            exp -= need;
            d.level++;
            d.talentPoints += talentPointsAt(d.level);
            gained++;
         }
         d.exp = d.level >= max ? Math.min(exp, PlayerStats.expToNextLevel(d.level)) : exp;
         return gained;
      }
   }

   /**
    * Trừ {@code percent}% EXP hiện có (của cấp hiện tại — không tụt cấp, không âm).
    *
    * @return số EXP đã trừ
    */
   public static long loseExpPercent(PlayerData d, int percent) {
      synchronized (d) {
         long lost = d.exp * percent / 100;
         d.exp -= lost;
         return lost;
      }
   }

   /** EXP còn thiếu để lên cấp kế. */
   public static long expToLevelUp(PlayerData d) {
      return Math.max(0, PlayerStats.expToNextLevel(d.level) - d.exp);
   }
}
