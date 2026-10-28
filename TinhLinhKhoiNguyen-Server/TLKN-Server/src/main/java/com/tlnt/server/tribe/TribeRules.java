package com.tlnt.server.tribe;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;

/**
 * Công thức Bộ lạc (spec "Spec hệ thống Bộ lạc"), hàm tĩnh không trạng thái ngoài {@link #config()} — dùng chung cho
 * {@link TribeService}, {@code item/ItemStats} (buff vào chỉ số) và {@code world/ZoneCombat} (buff theo HP lúc đánh).
 *
 * <h2>Cấp</h2>
 * Cấp 0 -> 1 cần {@value #FIRST_NEED} điểm cống hiến, mỗi cấp sau cần thêm {@value #NEED_STEP} (cấp N cần
 * 3000 + 250 × (N − 1)), tối đa cấp {@value #MAX_LEVEL} (cộng dồn 1.537.500). Sức chứa = 10 + cấp. Ô kỹ năng: cấp 0 không
 * có, từ cấp 1 = 1 + cấp / 5, tối đa {@value #MAX_SKILLS} (đủ ở cấp 45).
 *
 * <h2>10 kỹ năng ({@code tribe_skill.id})</h2>
 * Chỉ số tĩnh cộng vào chỉ số nhân vật như 1 nguồn riêng (như bộ sưu tập: "+X" cộng, tổng "+X%" nhân 1 lần —
 * {@link #addStats}): 3 +500 Chí mạng +500 Chính xác, 5 +2% Hấp thụ, 6 +10% HP / MP, 7 +20% Tấn công lên quái (chỉ áp lên
 * quái vì chỉ số này không dùng khi đánh người), 8 Hồi 2% HP / MP tối đa mỗi 5 s (vào dòng Hồi HP / MP mỗi 5 s),
 * 2 +100 Giáp / người đứng cạnh (tối đa 2000), 4 +2% Thưởng kinh nghiệm / người đứng cạnh (tối đa 20%).
 * Theo HP HIỆN TẠI nên tính lúc đánh (không hiện ở bảng chỉ số): 1 HP dưới 50% -> sát thương +10%, 9 HP trên 50% ->
 * +5% Sát thương chí mạng, 10 mỗi 10% HP đã mất giảm 2% sát thương nhận vào (bậc thang, tối đa
 * {@code tribe.enduranceMaxPercent}).
 */
public final class TribeRules {

   public static final int MAX_LEVEL = 100;
   public static final long FIRST_NEED = 3_000;
   public static final long NEED_STEP = 250;
   public static final int BASE_MEMBERS = 10;
   public static final int MAX_SKILLS = 10;
   /** Vai trò = thứ tự {@code TribeMember.Role} client: LEADER, DEPUTY, MEMBER. */
   public static final int ROLE_LEADER = 0;
   public static final int ROLE_DEPUTY = 1;
   public static final int ROLE_MEMBER = 2;
   /** {@code tribe_emblem.id} mặc định Cờ lệnh (vĩnh viễn). */
   public static final int DEFAULT_EMBLEM = 1;

   // ---- tribe_skill.id
   public static final int SKILL_BLOOD_RAGE = 1;
   public static final int SKILL_UNITY = 2;
   public static final int SKILL_HUNTER_MARK = 3;
   public static final int SKILL_SCHOLAR = 4;
   public static final int SKILL_SPIRIT = 5;
   public static final int SKILL_HIDDEN_POWER = 6;
   public static final int SKILL_PREDATOR = 7;
   public static final int SKILL_LIFE_FLOW = 8;
   public static final int SKILL_WEAK_POINT = 9;
   public static final int SKILL_ENDURANCE = 10;

   static final int UNITY_ARMOR = 100;
   static final int UNITY_ARMOR_MAX = 2_000;
   static final int SCHOLAR_EXP = 2;
   static final int SCHOLAR_EXP_MAX = 20;
   static final int BLOOD_RAGE_PERCENT = 10;
   static final int WEAK_POINT_CRIT = 5;
   static final int ENDURANCE_STEP = 2;
   static final int LIFE_FLOW_PERCENT = 2;

   private static volatile TribeConfig config = TribeConfig.DEFAULTS;

   private TribeRules() {
   }

   public static void configure(TribeConfig value) {
      config = value;
   }

   public static TribeConfig config() {
      return config;
   }

   // ------------------------------------------------------------------ cấp

   /** Điểm cống hiến cần để lên cấp {@code toLevel} (từ cấp {@code toLevel - 1}). */
   public static long need(int toLevel) {
      return FIRST_NEED + NEED_STEP * (Math.max(1, toLevel) - 1);
   }

   public static int maxMembers(int level) {
      return BASE_MEMBERS + level;
   }

   public static int skillSlots(int level) {
      return level < 1 ? 0 : Math.min(MAX_SKILLS, 1 + level / 5);
   }

   // ------------------------------------------------------------------ buff vào chỉ số (luồng session)

   /**
    * Cộng dòng chỉ số bộ lạc của {@code d} (kỹ năng tĩnh + biểu tượng + kỹ năng "đứng cạnh") vào {@code lines} (theo id
    * {@code stat_type}) — {@code ItemStats} gộp như 1 nguồn.
    */
   public static void addStats(long[] lines, PlayerData d) {
      TribeTag tag = d.tribe;
      if (tag == null) {
         return;
      }
      if (tag.has(SKILL_HUNTER_MARK)) {
         lines[11] += 500; // Chí mạng
         lines[21] += 500; // Chính xác
      }
      if (tag.has(SKILL_SPIRIT)) {
         lines[45] += 2; // % Hấp thụ sát thương
      }
      if (tag.has(SKILL_HIDDEN_POWER)) {
         lines[4] += 10; // % HP
         lines[6] += 10; // % MP
      }
      if (tag.has(SKILL_PREDATOR)) {
         lines[10] += 20; // % Tấn công lên quái
      }
      int nearby = Math.max(0, d.tribeNearby);
      if (tag.has(SKILL_UNITY)) {
         lines[15] += Math.min(UNITY_ARMOR_MAX, (long) UNITY_ARMOR * nearby);
      }
      if (tag.has(SKILL_SCHOLAR)) {
         lines[49] += Math.min(SCHOLAR_EXP_MAX, (long) SCHOLAR_EXP * nearby);
      }
      for (int[] line : tag.emblemLines()) {
         if (line.length >= 2 && line[0] > 0 && line[0] < lines.length) {
            lines[line[0]] += line[1];
         }
      }
   }

   /** Kỹ năng 8: % HP / MP tối đa cộng vào dòng Hồi mỗi 5 s. */
   public static int regenPercent(PlayerData d) {
      TribeTag tag = d.tribe;
      return tag != null && tag.has(SKILL_LIFE_FLOW) ? LIFE_FLOW_PERCENT : 0;
   }

   // ------------------------------------------------------------------ buff theo HP (luồng tick khu, lúc trúng đòn)

   /** Kỹ năng 1: HP dưới 50% -> sát thương thô của đòn +10%. */
   public static long bloodRage(PlayerData d, PlayerStats s, long raw) {
      TribeTag tag = d.tribe;
      if (tag == null || raw <= 0 || !tag.has(SKILL_BLOOD_RAGE) || s == null || d.hp * 2 >= s.maxHp) {
         return raw;
      }
      return raw + raw * BLOOD_RAGE_PERCENT / 100;
   }

   /** Kỹ năng 9: HP trên 50% -> + % Sát thương chí mạng (cộng vào chỉ số lúc chí mạng). */
   public static int weakPointCrit(PlayerData d, PlayerStats s) {
      TribeTag tag = d.tribe;
      return tag != null && tag.has(SKILL_WEAK_POINT) && s != null && d.hp * 2 > s.maxHp ? WEAK_POINT_CRIT : 0;
   }

   /** Kỹ năng 10: % giảm sát thương nhận vào = 2% × số mốc 10% HP đã mất, tối đa {@code tribe.enduranceMaxPercent}. */
   public static int endurancePercent(PlayerData d, PlayerStats s) {
      TribeTag tag = d.tribe;
      if (tag == null || !tag.has(SKILL_ENDURANCE) || s == null || s.maxHp <= 0) {
         return 0;
      }
      long lost = Math.max(0, s.maxHp - Math.max(0, d.hp)) * 100 / s.maxHp;
      return (int) Math.min(config.enduranceMaxPercent(), lost / 10 * ENDURANCE_STEP);
   }

   /** Trừ phần kỹ năng 10 khỏi sát thương nhận vào. */
   public static long endurance(PlayerData d, PlayerStats s, long damage) {
      int percent = endurancePercent(d, s);
      return percent > 0 && damage > 0 ? damage - damage * percent / 100 : damage;
   }
}
