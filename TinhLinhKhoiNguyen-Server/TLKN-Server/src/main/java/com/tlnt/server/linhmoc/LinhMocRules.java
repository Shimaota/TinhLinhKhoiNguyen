package com.tlnt.server.linhmoc;

import com.tlnt.server.combat.MobRules;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.player.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Luật phó bản Rừng Linh Mộc (không trạng thái ngoài cấu hình {@link #configure} lúc khởi động). Mọi phép tính bằng
 * {@code double}, chỉ làm tròn ở bước cuối.
 *
 * <pre>
 * cấp quái = min(cấp cao nhất nhóm lúc mở, maxLevel)        — khoá tới hết lượt
 * B (unit) = hpBase × cấp^hpExponent                          (144 × L^2,25 — gốc HP, double)
 * HP       = round(hệ_số_HP × B / hpRound) × hpRound            (làm tròn tới 50 như game gốc)
 * u        = B / damageDivisor                                 (/ 380 — cấp 18 ≈ 253, cấp 28 ≈ 683)
 * ST quái  = hệ_số_ST × u × 2200 / (2200 + Giáp) × rand(0.95, 1.05) × (1 − giảm ST thuốc) -> max(1, floor)
 * EXP      = floor(floor(sát thương thật × exp.dungeon × cấp lượt) × (1 + EXP% / 100)) × exp.rate   (mọi quái, không chia;
 *            linhmoc.expFactor &gt; 0 thì thay exp.dungeon × cấp bằng hệ số cố định đó)
 * </pre>
 */
public final class LinhMocRules {

   private LinhMocRules() {
   }

   /** Sai số double trước floor (vd 1000 × 0.36 có thể ra 359,99999…). */
   private static final double EPS = 1e-6;

   private static volatile LinhMocConfig config = LinhMocConfig.defaults();

   /** Gọi 1 lần lúc khởi động, TRƯỚC khi tạo {@code World} (GameMap / MapPackets đọc dãy map). */
   public static void configure(LinhMocConfig c) {
      config = c;
   }

   public static LinhMocConfig config() {
      return config;
   }

   public static boolean isDungeonMap(int mapId) {
      return config.maps().contains(mapId);
   }

   /** Map đầu (nơi vào / hồi sinh). */
   public static int firstMap() {
      return config.maps().get(0);
   }

   /** Thứ tự map trong phó bản (0 = map đầu), -1 = không phải map phó bản. */
   public static int stage(int mapId) {
      return config.maps().indexOf(mapId);
   }

   /**
    * Lượt còn lại hôm nay ({@code today} = LocalDate.toEpochDay giờ server): 1 lượt / ngày — đã đi trong ngày
    * {@code today} thì 0, sang 0:00 là ngày khác -> 1 (giống Tế đàn, không cần reset lúc nửa đêm).
    */
   public static int turnsLeft(PlayerData d, long today) {
      return d.linhMocDay == today ? 0 : 1;
   }

   public static int mobLevel(int highestPartyLevel) {
      return Math.max(1, Math.min(highestPartyLevel, config.maxLevel()));
   }

   /** Gốc B của cấp {@code mobLevel} = 144 × L^2,25 (chưa làm tròn — HP làm tròn theo từng loại quái). */
   public static double unit(int mobLevel) {
      return unit(config, mobLevel);
   }

   static double unit(LinhMocConfig c, int mobLevel) {
      return c.hpBase() * Math.pow(mobLevel, c.hpExponent());
   }

   /** Có hệ số cho loại quái này không (không có: giữ chỉ số đặt sẵn trong map + công thức quái thường). */
   public static boolean hasMob(int mobTemplateId) {
      return config.mobs().containsKey(mobTemplateId);
   }

   /** HP quái = round(hệ số × B / 50) × 50 (Excel: =ROUND(144 * hệ_số * L^2.25 / 50, 0) * 50); -1 = không có hệ số. */
   public static long mobHp(int mobTemplateId, double unit) {
      LinhMocConfig.MobCoef coef = config.mobs().get(mobTemplateId);
      if (coef == null) {
         return -1;
      }
      int step = config.hpRound();
      return Math.max(step, Math.round(coef.hp() * unit / step) * step);
   }

   /**
    * 1 đòn quái đánh người ({@code roll} = rand(0.95, 1.05), {@code reducePercent} = giảm ST nhận từ quái của thuốc,
    * vd Cứng cáp I = 35). 0 = loại quái không có hệ số (người gọi dùng công thức quái thường). Chưa kẹp theo HP còn —
    * {@code PlayerData.damageHp} kẹp.
    */
   public static long mobHitsPlayer(int mobTemplateId, double unit, long armor, long reducePercent, double roll) {
      LinhMocConfig.MobCoef coef = config.mobs().get(mobTemplateId);
      if (coef == null) {
         return 0;
      }
      double u = unit / config.damageDivisor();
      double dmg = coef.damage() * u * com.tlnt.server.combat.StatRules.armorFactor(armor, 0) * roll;
      dmg *= 1 - Math.min(90, Math.max(0, reducePercent)) / 100.0;
      return Math.max(1, (long) Math.floor(dmg + EPS));
   }

   /**
    * EXP 1 đòn từ sát thương thật đã trừ vào quái cấp {@code mobLevel} (= cấp lượt): hệ số / 1 sát thương =
    * {@link MobRules#dungeonExpPerDamage} ({@code exp.dungeon} × cấp), hoặc {@code linhmoc.expFactor} nếu &gt; 0.
    */
   public static long hitExp(long dealt, int mobLevel, long expBonusPercent, int expRate) {
      if (dealt <= 0) {
         return 0;
      }
      double perDamage = config.expFactor() > 0 ? config.expFactor() : MobRules.dungeonExpPerDamage(mobLevel);
      long base = (long) Math.floor(dealt * perDamage + EPS);
      long withBonus = (long) Math.floor(base * (1 + Math.max(0, expBonusPercent) / 100.0) + EPS);
      return withBonus * Math.max(1, expRate);
   }

   /**
    * Quái của 1 khu phó bản ở cấp {@code mobLevel}: loại có hệ số -> quái thường cấp đó, HP theo unit; loại không có
    * hệ số -> giữ nguyên như map đặt sẵn. Id / vị trí giữ nguyên.
    */
   public static List<MapTemplate.MobInstance> scaleMobs(List<MapTemplate.MobInstance> mobs, int mobLevel,
         double unit) {
      List<MapTemplate.MobInstance> out = new ArrayList<>(mobs.size());
      for (MapTemplate.MobInstance mob : mobs) {
         long hp = mobHp(mob.spawn().id(), unit);
         out.add(hp < 0 ? mob
               : new MapTemplate.MobInstance(mob.id(), mob.spawn().withLevel(mobLevel, hp), mob.x(), mob.y()));
      }
      return out;
   }

   // ------------------------------------------------------------------ boss

   /** Tinh linh Thần mộc: char_template 10 (skeleton chars/17), class 7, skin duy nhất của skeleton. */
   static final int SPIRIT_CHAR_TEMPLATE = 10;
   static final int SPIRIT_CLASS = 7;
   static final String[] SPIRIT_SKINS = {"stick/lv2"};
   /** Chí mạng 20% (phần vạn), +18% (phần nghìn), né / chính xác 20% (phần vạn), tốc chạy 1350 px/s. */
   static final int SPIRIT_CRIT = 2000;
   static final int SPIRIT_CRIT_DAMAGE = 180;
   static final int SPIRIT_EVASION = 2000;
   static final int SPIRIT_ACCURACY = 2000;
   static final long SPIRIT_MOVE_SPEED = 1350;
   /**
    * Vòng chiêu (id op 21): 32 {@code stick/skill1} đánh thường 1 mục tiêu, 33 {@code stick/skill2} chém diện rộng phía
    * trước, 34 {@code stick/skill3} đập đất hất tung xung quanh, 35 {@code stick/skill4} rễ cây trói 5 người 10 s.
    */
   static final int[] SPIRIT_ROTATION = {32, 32, 33, 32, 34, 32, 35};
   /** % sát thương gốc từng chiêu (35 trói: không gây sát thương). */
   static final java.util.Map<Integer, Integer> SPIRIT_PERCENTS = java.util.Map.of(32, 100, 33, 150, 34, 200, 35, 0);
   /**
    * Thời gian ra chiêu = độ dài animation skeleton chars/17 ({@code stick/skill1} 0,867 s, sự kiện atk 0,333 s;
    * {@code skill2} 1,333 s; {@code skill3} 1,0 s, rung màn hình 40% = 0,4 s; {@code skill4} 1,367 s, sự kiện skill_up 0,567 s):
    * {skill: [ms ra chiêu, ms lúc trúng]}. Không cộng thời gian nghỉ ngoài animation và hồi chiêu riêng của skill.
    */
   static final java.util.Map<Integer, long[]> SPIRIT_TIMINGS = java.util.Map.of(32, new long[] {867, 333},
         33, new long[] {1333, 600}, 34, new long[] {1000, 400}, 35, new long[] {1367, 567});
   static final long SPIRIT_GAP_MS = 0;

   /** Map có Tinh linh Thần mộc = map cuối của dãy. */
   public static boolean isSpiritMap(int mapId) {
      List<Integer> maps = config.maps();
      return mapId == maps.get(maps.size() - 1);
   }

   /** HP boss = round(hệ số × B / 50) × 50 (như quái). */
   static long bossHp(double coef, double unit) {
      int step = config.hpRound();
      return Math.max(step, Math.round(coef * unit / step) * step);
   }

   /**
    * Chỉ số Tinh linh Thần mộc của lượt (cấp lượt {@code mobLevel}, gốc {@code unit}): HP = MP = spiritHp × B, đòn thường
    * (100%) = spiritDamage × u, Giáp = Xuyên giáp = cấp × armorPerLevel, EXP tối đa = mốc lên cấp của cấp đó. Đứng ở chỗ
    * con quái cuối map ({@code npcId} 0). Không rơi quà (để sau).
    */
   public static com.tlnt.server.altar.AltarRules.BossSpec spiritSpec(int mapId, int mobLevel, double unit) {
      LinhMocConfig c = config;
      long hp = bossHp(c.spiritHp(), unit);
      long hit = Math.max(1, (long) Math.floor(c.spiritDamage() * unit / c.damageDivisor() + EPS));
      long armor = (long) mobLevel * c.armorPerLevel();
      return new com.tlnt.server.altar.AltarRules.BossSpec(mapId, 0, c.text("spiritName"), SPIRIT_CHAR_TEMPLATE,
            SPIRIT_CLASS, mobLevel, hp, hp, 5, SPIRIT_SKINS, hit, SPIRIT_CRIT, SPIRIT_CRIT_DAMAGE, armor, armor,
            SPIRIT_EVASION, SPIRIT_ACCURACY, hit, SPIRIT_ROTATION, null, List.of(), SPIRIT_MOVE_SPEED, SPIRIT_PERCENTS,
            SPIRIT_TIMINGS, SPIRIT_GAP_MS);
   }

   // ------------------------------------------------------------------ quà

   /** {@code item_template.id} Quả Linh Mộc (quà hoàn thành lượt). */
   public static final int FRUIT = 288;

   /** Hệ số quà của cấp lượt {@code level} (bậc cao nhất có {@code fromLevel} ≤ cấp; thấp hơn mọi bậc -> bậc đầu). */
   public static double tierMult(int level) {
      List<LinhMocConfig.Tier> tiers = config.tiers();
      double mult = tiers.isEmpty() ? 1 : tiers.get(0).mult();
      for (LinhMocConfig.Tier t : tiers) {
         if (level >= t.fromLevel()) {
            mult = t.mult();
         }
      }
      return mult;
   }

   /**
    * Tung quà theo bảng {@code table} ở cấp lượt {@code level}: mỗi dòng qua tỉ lệ thì ra {@code piles} món, số lượng
    * random [min, max] × hệ số (theo {@code scale}), tối thiểu 1. Mọi món KHOÁ.
    */
   public static List<com.tlnt.server.player.ItemData> rollLoot(List<LinhMocConfig.Loot> table, int level,
         java.util.Random random) {
      double mult = tierMult(level);
      List<com.tlnt.server.player.ItemData> out = new ArrayList<>();
      for (LinhMocConfig.Loot loot : table) {
         if (loot.chance() < 100 && random.nextDouble() * 100 >= loot.chance()) {
            continue;
         }
         double factor = switch (loot.scale()) {
            case 's' -> mult;
            case 'h' -> 1 + (mult - 1) / 2;
            default -> 1;
         };
         for (int i = 0; i < loot.piles(); i++) {
            int base = loot.max() > loot.min() ? loot.min() + random.nextInt(loot.max() - loot.min() + 1) : loot.min();
            int qty = (int) Math.max(1, Math.floor(base * factor + EPS));
            com.tlnt.server.player.ItemData item = com.tlnt.server.player.ItemData.of(loot.itemId(), qty);
            item.locked = true;
            out.add(item);
         }
      }
      return out;
   }

   /** Quả Linh Mộc mỗi thành viên khi lượt kết thúc = floor(cấp lượt × số map đã dọn / số map phó bản). */
   public static int completionFruits(int level, int clearedMaps) {
      int total = config.maps().size();
      return (int) Math.floor((double) level * Math.min(clearedMaps, total) / total + EPS);
   }
}
