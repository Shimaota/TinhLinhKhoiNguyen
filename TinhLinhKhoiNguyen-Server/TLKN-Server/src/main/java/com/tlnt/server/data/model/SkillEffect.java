package com.tlnt.server.data.model;

/**
 * 1 dòng bảng {@code skill_effect} (chỉ server dùng): con số của 1 cấp kỹ năng mà {@code skill_level} chỉ có dạng
 * chữ ({@code effect_description}). Sinh bằng {@code tools/gen_skill_effect.py}. Cột NULL trong SQL = 0 / null ở đây.
 *
 * @param damagePercent % Tấn công mỗi đòn (đòn chính); 0 = kỹ năng không gây sát thương (buff)
 * @param hits          số đòn, ≥ 1 (gộp 1 gói hay mỗi đòn 1 gói: {@code CombatService})
 * @param bonusStat     "hp" | "mp" | "accuracy" | "crit" | "evasion" cho {@code bonusPercent}; null = không có
 * @param status        "ice" | "weaken" | "lightning" | "stun" | "root"; null = không có
 */
public record SkillEffect(int skillId, int level, int damagePercent, int hits, int aoeTargets, int aoePercent,
      int armorPen, int accuracy, int critDamagePercent, int lifestealPercent, int bleedPercent, int bonusPercent,
      String bonusStat, String status, int statusPower, int statusMs, int healPercent, int damageReductionPercent,
      int reflectPercent, int attackSpeedPercent, int moveSpeedPercent, int illusionPercent, int durationMs) {

   /** Khoá tra cứu ({@code skill_id}, {@code level}) -> 1 số. */
   public static long key(int skillId, int level) {
      return (long) skillId << 32 | (level & 0xFFFFFFFFL);
   }

   public long key() {
      return key(skillId, level);
   }
}
