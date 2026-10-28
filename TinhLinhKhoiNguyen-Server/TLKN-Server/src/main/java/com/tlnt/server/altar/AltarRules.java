package com.tlnt.server.altar;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;

import java.util.List;

/**
 * Luật phó bản "Tế đàn Thiên tích" (bất biến, không trạng thái): 7 map 34 → 40 đi lần lượt, mỗi map 1 loại Cận vệ
 * Thiên tích (mob 55-61, cấp + HP đặt sẵn trong {@code map_template.mobs}), không hồi sinh, solo, 30 phút / lượt,
 * 1 lượt / ngày. Rương gỗ cấp N (item 311 + N - 1) = đã diệt hết N map đầu tiên.
 */
public final class AltarRules {

   private AltarRules() {
   }

   /** Map 1 (Tế đàn Cấm vệ) .. map 7 (Trung tâm Tế đàn). */
   public static final int FIRST_MAP = 34;
   public static final int LAST_MAP = 40;
   public static final int STAGES = LAST_MAP - FIRST_MAP + 1;
   /** Thời gian 1 lượt (tính từ lúc bấm "Tham gia", thoát ra ngoài vẫn chạy). */
   public static final long RUN_MS = 30 * 60_000L;
   /** Số lượt Tế đàn chạy CÙNG LÚC tối đa (cả server): đủ thì từ chối lượt mới, không trừ lượt (vào lại lượt đang dở vẫn được). */
   public static final int MAX_RUNS = 100;
   /** Điểm hoạt động ({@code PlayerData.dynamicPoints}, op 23 sub 9) nhận khi bắt đầu 1 lượt mới. */
   public static final int JOIN_DYNAMIC_POINTS = 25;
   /** {@code item_template.id} Rương gỗ cấp 1 (cấp N = 311 + N - 1, tới cấp 7 = 317). */
   public static final int CHEST_LV1 = 311;
   /** Nghi thức Ấn linh: giá (lam thạch) + % cộng thêm (chỉ có tác dụng trong Tế đàn). */
   public static final long SEAL_PRICE = 10;
   public static final int SEAL_PERCENT = 15;

   /**
    * Lượt còn lại hôm nay ({@code today} = LocalDate.toEpochDay giờ server): mỗi ngày 1 lượt — đã đi trong ngày
    * {@code today} thì 0, sang 0:00 là ngày khác -> 1 (không cần việc reset lúc nửa đêm).
    */
   public static int turnsLeft(PlayerData d, long today) {
      return d.altarDay == today ? 0 : 1;
   }

   public static boolean isAltarMap(int mapId) {
      return mapId >= FIRST_MAP && mapId <= LAST_MAP;
   }

   /** Thứ tự map trong Tế đàn: 34 -> 1 .. 40 -> 7. */
   public static int stage(int mapId) {
      return mapId - FIRST_MAP + 1;
   }

   /** Map của tầng {@code stage} (1..7). */
   public static int mapOf(int stage) {
      return FIRST_MAP + stage - 1;
   }

   /** Rương nhận được khi đã diệt hết {@code cleared} tầng; 0 = không có rương. */
   public static int chestItem(int cleared) {
      return cleared <= 0 ? 0 : CHEST_LV1 + Math.min(cleared, STAGES) - 1;
   }

   /**
    * Sát thương GỐC 1 đòn của Cận vệ Thiên tích cấp {@code level} (bảng người dùng cho, cấp 1..100) — vẫn giảm theo Giáp
    * người chơi ×2200/(2200+Giáp) và dao động ±5% như quái thường ({@code combat/MobRules#tableHitsPlayer}).
    */
   public static long baseDamage(int level) {
      int i = Math.max(1, Math.min(DAMAGE.length, level)) - 1;
      return DAMAGE[i];
   }

   private static final long[] DAMAGE = {
         150, 200, 250, 300, 350, 400, 450, 500, 550, 600, // 1-10
         650, 689, 729, 768, 807, 846, 886, 925, 964, 1003, // 11-20
         1043, 1082, 1121, 1160, 1200, 1300, 1343, 1386, 1429, 1471, // 21-30
         1514, 1557, 1600, 1643, 1686, 1729, 1771, 1814, 1857, 1900, // 31-40
         2000, 2050, 2100, 2150, 2200, 2250, 2300, 2350, 2400, 2450, // 41-50
         2467, 2517, 2567, 2617, 2700, 2800, 2850, 2900, 2950, 3000, // 51-60
         3050, 3100, 3150, 3200, 3250, 3300, 3350, 3400, 3450, 3500, // 61-70
         3600, 3650, 3700, 3750, 3800, 3567, 3617, 3667, 3717, 3767, // 71-80
         3817, 3867, 3917, 3967, 4300, 4400, 4443, 4486, 4529, 4571, // 81-90
         4614, 4657, 4700, 4743, 4786, 4829, 4871, 4914, 4957, 5000, // 91-100
   };

   // ------------------------------------------------------------------ boss

   /**
    * Boss của 1 tầng: người chơi giả (op 3 loại thực thể 8, cờ Thử thách -> ai cũng chọn làm mục tiêu được), xuất hiện ở
    * chỗ NPC {@code npcId} của map (0 = CUỐI map, trước cửa sang map sau), đuổi theo người chơi + ra chiêu theo vòng
    * {@code rotation} ({@code world/AltarBoss}). Phải hạ boss + hết quái mới mở cửa. Tỉ lệ chí mạng / né / chính xác theo
    * PHẦN VẠN (1000 = 10%); sát thương chí mạng theo PHẦN NGHÌN (180 = +18%, 250 = +25%).
    *
    * <p>Sát thương 1 đòn = {@code hitDamage} × % chiêu × 2200/(2200+Giáp) ±5% (Tấn công {@code attack} chỉ để hiện).
    *
    * @param hitDamage sát thương gốc 1 đòn thường (100%) trước Giáp — chỉnh độ mạnh từng boss ở đây
    * @param rotation  vòng chiêu = id kỹ năng op 21 client chơi: {@link #MOVE_ATTACK} 1 (đánh thường {@code attack_1 /
    *                  attack_2} xen kẽ), 32 {@code stick/skill1}, 33 {@code stick/skill2}, 34 {@code stick/skill3} (hất
    *                  người trúng nảy lên), 40 {@code stick/skill9} (lông vũ)
    * @param hurtChat  chat khi bị đánh (null = không chat)
    * @param drops     quà rơi xuống đất (khoá) cho người kết liễu
    */
   public record BossSpec(int mapId, int npcId, String name, int charTemplateId, int classId, int level, long hp,
         long mp, int weaponLevel, String[] skins, long attack, int critChance, int critDamagePercent, long armor,
         long armorPen, int evasion, int accuracy, long hitDamage, int[] rotation, String hurtChat,
         List<BossDrop> drops, long moveSpeed, java.util.Map<Integer, Integer> percents,
         java.util.Map<Integer, long[]> timings, long gapMs) {

      /** Như canonical, thời gian ra chiêu mặc định (boss Tế đàn). */
      public BossSpec(int mapId, int npcId, String name, int charTemplateId, int classId, int level, long hp,
            long mp, int weaponLevel, String[] skins, long attack, int critChance, int critDamagePercent, long armor,
            long armorPen, int evasion, int accuracy, long hitDamage, int[] rotation, String hurtChat,
            List<BossDrop> drops, long moveSpeed, java.util.Map<Integer, Integer> percents) {
         this(mapId, npcId, name, charTemplateId, classId, level, hp, mp, weaponLevel, skins, attack, critChance,
               critDamagePercent, armor, armorPen, evasion, accuracy, hitDamage, rotation, hurtChat, drops, moveSpeed,
               percents, null, -1);
      }

      /** Boss Tế đàn: tốc chạy {@link #BOSS_MOVE_SPEED_STAT}, % chiêu mặc định ({@link #percent}). */
      public BossSpec(int mapId, int npcId, String name, int charTemplateId, int classId, int level, long hp,
            long mp, int weaponLevel, String[] skins, long attack, int critChance, int critDamagePercent, long armor,
            long armorPen, int evasion, int accuracy, long hitDamage, int[] rotation, String hurtChat,
            List<BossDrop> drops) {
         this(mapId, npcId, name, charTemplateId, classId, level, hp, mp, weaponLevel, skins, attack, critChance,
               critDamagePercent, armor, armorPen, evasion, accuracy, hitDamage, rotation, hurtChat, drops,
               BOSS_MOVE_SPEED_STAT, null);
      }

      /**
       * Thời gian ra chiêu {@code skill} (đứng yên, = độ dài animation) — bảng riêng {@code timings} ({skill: [ms ra chiêu,
       * ms lúc trúng]}) nếu có, không thì {@code def}.
       */
      public long castMs(int skill, long def) {
         long[] t = timings == null ? null : timings.get(skill);
         return t == null ? def : t[0];
      }

      /** Lúc đòn trúng trong animation (ms từ lúc ra chiêu); xem {@link #castMs}. */
      public long hitMs(int skill, long def) {
         long[] t = timings == null ? null : timings.get(skill);
         return t == null || t.length < 2 ? def : t[1];
      }

      /** Nghỉ giữa 2 chiêu (ms); âm = mặc định. */
      public long gap(long def) {
         return gapMs < 0 ? def : gapMs;
      }

      /** % sát thương gốc của chiêu {@code skill} (id op 21): bảng riêng của boss nếu có, không thì mặc định. */
      public int percent(int skill) {
         if (percents != null && percents.containsKey(skill)) {
            return percents.get(skill);
         }
         return switch (skill) {
            case MOVE_ATTACK -> BOSS_ATTACK_PERCENT;
            case MOVE_SKILL1 -> BOSS_SKILL1_PERCENT;
            case MOVE_SKILL2 -> BOSS_SKILL2_PERCENT;
            case MOVE_SKILL3 -> BOSS_SKILL3_PERCENT;
            case MOVE_FEATHERS -> BOSS_FEATHER_PERCENT;
            default -> 0; // 35 trói: mặc định không gây sát thương
         };
      }

      /** Chỉ số gửi client (khối chỉ số op 3) + dùng khi người đánh boss ({@code PvpMath}). */
      public PlayerStats stats() {
         PlayerStats s = new PlayerStats();
         s.maxHp = hp;
         s.maxMp = mp;
         s.attackPower = attack;
         s.critChance = critChance;
         s.critDamagePercent = critDamagePercent;
         s.armor = armor;
         s.armorPen = armorPen;
         s.evasion = evasion;
         s.accuracy = accuracy;
         s.moveSpeed = moveSpeed;
         return s;
      }
   }

   /**
    * Quà rơi: {@code piles} món riêng rẽ (mỗi món nhặt 1 lần), mỗi món số lượng random [{@code min}, {@code max}], KHOÁ.
    * Vàng = item 1 (nhặt lên vào vàng khoá).
    */
   public record BossDrop(int templateId, int piles, int min, int max) {
   }

   /** Item vàng (khoá -> vàng khoá), Đá khởi nguyên, Đá Mặt trăng, Đá Mặt trời. */
   static final int GOLD = 1;
   static final int ORIGIN_STONE = 274;
   static final int MOON_STONE = 126;
   static final int SUN_STONE = 125;

   /** Chỉ số Tốc chạy (thông số gốc) = tốc đuổi thật của boss (px / s). */
   static final long BOSS_MOVE_SPEED_STAT = 1150;
   public static final float BOSS_CHASE_SPEED = BOSS_MOVE_SPEED_STAT;
   /** Điểm hoạt động cho người hạ boss. */
   public static final int BOSS_KILL_DYNAMIC_POINTS = 25;
   /** Id kỹ năng op 21: đánh thường (client tự xen kẽ attack_1 / attack_2). */
   public static final int MOVE_ATTACK = 1;
   public static final int MOVE_SKILL1 = 32;
   public static final int MOVE_SKILL2 = 33;
   public static final int MOVE_SKILL3 = 34;
   public static final int MOVE_FEATHERS = 40;
   /**
    * {@code stick/skill4} (op 21 id 35, không payload): TRÓI tối đa {@link #ROOT_TARGETS} người ngẫu nhiên quanh boss
    * {@link #ROOT_MS} ms (op 35 sub 0 — client {@code rootEffect} khoá di chuyển). Tinh linh Thần mộc (Rừng Linh Mộc).
    */
   public static final int MOVE_ROOT = 35;
   /**
    * {@code stick/skill10} (op 21 id 41, như skill1: byte loại mục tiêu, int id, long HP còn, long sát thương, bool chí
    * mạng): tia sét bắn 1 mục tiêu ở xa (client {@code BossSkill10Anim} + {@code ThunderboltProjectile}). Tinh linh Sấm
    * sét (Lôi vực) — boss có chiêu này trong {@code percents} thì dùng thay chiêu trói.
    */
   public static final int MOVE_BOLT = 41;
   /**
    * Chiêu đặc biệt của Tinh linh boss thế giới (op 21, KHÔNG payload — trừ 37): 36 {@code stick/skill5} Đầm lầy (suy
    * yếu 3 mục tiêu / 15 s), 37 {@code stick/skill6} Biển cả (bool true = dựng khiên nước, false = vỡ khiên —
    * {@code skill6_3}), 38 {@code stick/skill7} Phong thạch (choáng 3 mục tiêu / 15 s), 39 {@code stick/skill8} Băng giá
    * (đóng băng mọi mục tiêu xung quanh 15 s). Có trong
    * {@code percents} của boss thì dùng THAY chiêu trói ({@code world/AltarBoss.special}).
    */
   public static final int MOVE_WEAKEN = 36;
   public static final int MOVE_SHIELD = 37;
   public static final int MOVE_STUN = 38;
   public static final int MOVE_FREEZE = 39;
   /**
    * 43 {@code stick/skill11} (Cổ thần Dạ xoa, Trung thu): hút 20% HP hiện tại của tối đa 5 mục tiêu ngẫu nhiên và hồi
    * lại đúng lượng HP đã hút. Client decompile C3873 case 43: KHÔNG payload, chỉ chạy animation (C3976).
    */
   public static final int MOVE_SKILL11 = 43;
   public static final int ROOT_TARGETS = 3;
   public static final int ROOT_MS = 15_000;
   /** % sát thương gốc fallback: đánh thường / skill1, skill2, skill3, mỗi lông vũ. */
   public static final int BOSS_ATTACK_PERCENT = 100;
   public static final int BOSS_SKILL1_PERCENT = 100;
   public static final int BOSS_SKILL2_PERCENT = 150;
   public static final int BOSS_SKILL3_PERCENT = 200;
   public static final int BOSS_FEATHER_PERCENT = 500;
   public static final int BOSS_FEATHERS = 50;

   private static final int[] SKY_ROTATION = {MOVE_SKILL1, MOVE_SKILL2, MOVE_SKILL1, MOVE_SKILL3, MOVE_FEATHERS};
   private static final int[] GUARD_ROTATION = {MOVE_ATTACK, MOVE_ATTACK, MOVE_SKILL2, MOVE_ATTACK, MOVE_SKILL3};

   /** Tầng 1: Tinh Linh Bầu Trời (lần 1) — đứng ở chỗ Tư tế Buraen (NPC 17) của map 34. */
   public static final BossSpec SKY_SPIRIT_1 = new BossSpec(FIRST_MAP, 17, "Tinh Linh Bầu trời", 9, 7, 10, 250_000,
         250_000, 5, new String[] {"stick/lv7"}, 250, 1000, 180, 250, 2500, 1000, 1000, 300, SKY_ROTATION, "Á đau! á đau",
         List.of(new BossDrop(ORIGIN_STONE, 5, 5, 10), new BossDrop(GOLD, 5, 5_000, 20_000)));
   /** Tầng 3: Zyren — cuối map 36 (Tế đàn Cận vệ). */
   public static final BossSpec ZYREN = new BossSpec(36, 0, "Zyren", 15, 7, 25, 1_000_000, 1_000_000, 5,
         new String[] {"default"}, 500, 1000, 200, 300, 2500, 2000, 2000, 1_200, GUARD_ROTATION, null,
         List.of(new BossDrop(MOON_STONE, 10, 1, 3), new BossDrop(GOLD, 5, 5_000, 20_000)));
   /** Tầng 5: Vaelor — cuối map 38 (Tế đàn Thiết vệ). */
   public static final BossSpec VAELOR = new BossSpec(38, 0, "Vaelor", 16, 7, 25, 1_000_000, 1_000_000, 5,
         new String[] {"default"}, 750, 2000, 210, 350, 2500, 2000, 2000, 1_800, GUARD_ROTATION, null,
         List.of(new BossDrop(SUN_STONE, 10, 1, 3), new BossDrop(GOLD, 5, 5_000, 20_000)));
   /** Tầng 7: Tinh Linh Bầu Trời (lần 2, mạnh hơn, cùng bộ chiêu) — cuối map 40 (Trung tâm Tế đàn). */
   public static final BossSpec SKY_SPIRIT_2 = new BossSpec(LAST_MAP, 0, "Tinh Linh Bầu trời", 9, 7, 100, 5_000_000,
         5_000_000, 5, new String[] {"stick/lv7"}, 2000, 2000, 250, 350, 2500, 2000, 2000, 2_500, SKY_ROTATION, "Á đau! á đau",
         List.of(new BossDrop(ORIGIN_STONE, 5, 10, 20), new BossDrop(GOLD, 5, 10_000, 50_000)));

   private static final List<BossSpec> BOSSES = List.of(SKY_SPIRIT_1, ZYREN, VAELOR, SKY_SPIRIT_2);

   /** Boss của map Tế đàn, null = map không có boss. */
   public static BossSpec boss(int mapId) {
      for (BossSpec spec : BOSSES) {
         if (spec.mapId() == mapId) {
            return spec;
         }
      }
      return null;
   }

   /**
    * Ấn linh (gọi trong {@code PlayerStats.of}): +15% Tấn công lên quái, +15% Hút HP quái, +15% Hút MP quái — chỉ khi
    * đã làm nghi thức trong lượt đang chạy VÀ đang đứng ở map Tế đàn.
    */
   public static void applySeal(PlayerStats s, PlayerData player) {
      AltarRun state = player.altarRun;
      if (state == null || !state.sealed || !isAltarMap(player.mapId)
            || state.activeEnd(System.currentTimeMillis()) == 0) {
         return;
      }
      s.attackVsMob += s.attackVsMob * SEAL_PERCENT / 100;
      s.hpLeechVsMobPercent += SEAL_PERCENT;
      s.mpLeechVsMobPercent += SEAL_PERCENT;
   }
}
