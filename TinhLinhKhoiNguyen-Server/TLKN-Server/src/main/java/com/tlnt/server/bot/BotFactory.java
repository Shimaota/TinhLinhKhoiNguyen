package com.tlnt.server.bot;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.item.WeaponClass;
import com.tlnt.server.player.ClassSkills;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.skill.SkillService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Dựng {@link PlayerData} của 1 người chơi giả "như người thật cùng cấp":
 * <ul>
 *   <li>Class (vũ khí) ngẫu nhiên từ cấp 4. Điểm tiềm năng đúng tổng nhận được tới cấp đó ({@code Leveling}): phần dành
 *       cho kỹ năng nâng đều các chiêu class (đúng {@code required_level} / {@code upgrade_cost}), còn lại cộng tiềm năng
 *       theo 1 kiểu {@link Build}; thừa vài điểm như người chưa cộng hết. Lên cấp khi đang chơi: {@code BotService}
 *       cộng tiếp bằng đúng {@link SkillService} (như người bấm nút).</li>
 *   <li>Trang bị: chỉ đồ mua được ở shop NPC (chỉ số roll y như shop — {@link ShopService#goods}): mỗi ô món cấp cao nhất
 *       mặc được, đôi khi kém 1 bậc / thiếu đai-mũ-vòng ở cấp thấp. KHÔNG BAO GIỜ: bộ Thủ lĩnh (loại 40-47), vũ khí Bất
 *       hoại / Lục linh, Giày da cũ. Vũ khí Đá -> Bạc (25) -> Vàng (45). Nâng sao chỉ khi đã tới nhiệm vụ lò rèn, trần sao
 *       theo nhiệm vụ ({@link BotProgress#maxStars}), lệch về số nhỏ.</li>
 *   <li>Túi: Táo đỏ / xanh hợp cấp + Đùi gà + Cỏ hồi sinh gán ô dùng nhanh (bot dùng thuốc qua đúng ItemService).</li>
 *   <li>Thỉnh thoảng có ngoại trang / danh hiệu / thú / mặt nạ / đeo lưng DỄ KIẾM, có chỉ số ({@link BotCosmetics}).</li>
 * </ul>
 * Hàm thuần trên dữ liệu tĩnh — gọi trên bất kỳ luồng nào trước khi bot vào thế giới.
 */
final class BotFactory {

   /** Cấp bắt đầu có vũ khí / class (nhiệm vụ 5 "Chế Tạo Công Cụ"). */
   static final int CLASS_FROM_LEVEL = 4;
   private static final int[] CLASSES = {WeaponClass.AXE, WeaponClass.BOW, WeaponClass.SLINGSHOT, WeaponClass.SPEAR,
         WeaponClass.WHIP};
   static final int RED_APPLE = 214;
   static final int GREEN_APPLE = 224;
   static final int DRUMSTICK = 150;
   /** Giày da cũ (rác câu cá): không bao giờ mặc. */
   static final int OLD_BOOTS = 265;
   private static final int LEADER_TYPE_MIN = 40;
   private static final int LEADER_TYPE_MAX = 47;

   /**
    * Kiểu build: tỉ lệ điểm cho [Sinh lực, Trí lực, Nhanh nhẹn, Tập trung, Bạo kích, Sức mạnh] + phần điểm dành cho kỹ
    * năng (0..1).
    */
   record Build(String name, double[] talents, double skillShare) {
   }

   static final Build[] BUILDS = {
         new Build("Công", new double[] {0.25, 0.05, 0, 0.15, 0.10, 0.45}, 0.65),
         new Build("Trâu", new double[] {0.50, 0.05, 0.15, 0, 0, 0.30}, 0.60),
         new Build("Chí mạng", new double[] {0.15, 0.05, 0, 0.15, 0.35, 0.30}, 0.65),
         new Build("Kỹ năng", new double[] {0.30, 0.20, 0, 0.10, 0.10, 0.30}, 0.75)};

   private static volatile Catalog catalog;

   private BotFactory() {
   }

   private record Catalog(Map<Integer, List<ItemPack.ItemTemplate>> armorBySlot,
         Map<Integer, List<Integer>> weaponsByClass) {
   }

   /** Vũ khí bot được cầm: không Bất hoại / Lục linh (đồ rèn hiếm), không cần câu. */
   static boolean allowedWeapon(ItemPack.ItemTemplate t) {
      String n = t.name() == null ? "" : t.name();
      return t.typeId() != 44 && !n.contains("Bất hoại") && !n.contains("Lục linh");
   }

   /** Đồ mặc bot được dùng: không món Thủ lĩnh nào, không Giày da cũ. */
   static boolean allowedArmor(ItemPack.ItemTemplate t) {
      String n = t.name() == null ? "" : t.name().toLowerCase(java.util.Locale.ROOT);
      return t.id() != OLD_BOOTS && (t.typeId() < LEADER_TYPE_MIN || t.typeId() > LEADER_TYPE_MAX)
            && !n.contains("thủ lĩnh"); // cả Vòng thủ lĩnh (loại 10, đổi bằng Huy hiệu Thủ lĩnh ở shop Tư tế)
   }

   private static Catalog catalog() {
      Catalog c = catalog;
      if (c == null) {
         Map<Integer, List<ItemPack.ItemTemplate>> armor = new HashMap<>();
         Map<Integer, List<Integer>> weapons = new HashMap<>();
         for (ItemPack.ItemTemplate t : GameDataCache.item().itemTemplates()) {
            ItemPack.ItemType type = GameDataCache.itemType(t.typeId());
            if (type == null || !type.equippable() || type.equipSlot() < 0) {
               continue;
            }
            if (type.equipSlot() == Appearance.SLOT_WEAPON) {
               int cls = WeaponClass.of(ItemData.of(t.id(), 1));
               if (cls != WeaponClass.NONE && allowedWeapon(t)) {
                  weapons.computeIfAbsent(cls, k -> new ArrayList<>()).add(t.id());
               }
            } else if (type.equipSlot() <= 7 && allowedArmor(t)) { // 8 / 9 = phụ kiện có lượt: bỏ
               armor.computeIfAbsent(type.equipSlot(), k -> new ArrayList<>()).add(t);
            }
         }
         weapons.values().forEach(l -> l.sort(Integer::compare)); // Đá < Bạc < Vàng
         catalog = c = new Catalog(armor, weapons);
      }
      return c;
   }

   /**
    * Nhân vật bot cấp {@code level}, chưa có map / vị trí (người gọi đặt {@code mapId}, {@code x}, {@code y}).
    * {@code accountId} âm — không trùng tài khoản thật.
    */
   static PlayerData create(int id, String name, int level, Build build, BotCosmetics looks, BotConfig cfg, Random rnd,
         long now) {
      PlayerData d = new PlayerData(id, -id);
      d.bot = true;
      d.name = name;
      d.gender = Gender.RINN + rnd.nextInt(3);
      d.level = Math.max(1, Math.min(level, com.tlnt.server.player.Leveling.maxLevel()));
      d.exp = (long) (PlayerStats.expToNextLevel(d.level) * rnd.nextDouble() * 0.9);
      d.classId = d.level >= CLASS_FROM_LEVEL ? CLASSES[rnd.nextInt(CLASSES.length)] : WeaponClass.NONE;
      d.talents = new int[GameDataCache.skill().talents().size()];
      d.settings = settings(rnd);
      d.sessionStartAt = now;
      d.playTimeSeconds = (long) d.level * d.level * 600 + rnd.nextInt(36_000);
      // Vàng đủ trả phí nâng kỹ năng khi lên cấp (như người chơi để dành), cộng thêm ít lẻ.
      d.gold = (long) d.level * d.level * (2_000 + rnd.nextInt(6_000)) + rnd.nextInt(50_000);
      ClassSkills.apply(d);
      spendPoints(d, build, rnd);
      skillBar(d);
      gear(d, rnd, now);
      bag(d, rnd);
      collections(d, looks, cfg, rnd, now);
      refresh(d);
      return d;
   }

   static void refresh(PlayerData d) {
      ItemService.refreshLook(d);
      PlayerStats stats = d.stats;
      d.setHpMp(stats.maxHp, stats.maxMp);
      d.activityPoints = stats.maxStamina;
   }

   static Build pickBuild(Random rnd) {
      return BUILDS[rnd.nextInt(BUILDS.length)];
   }

   // ------------------------------------------------------------------ cài đặt tự đánh

   private static PlayerSettings settings(Random rnd) {
      PlayerSettings s = PlayerSettings.defaults();
      java.util.Arrays.fill(s.autoSkillSlotEnabled, true);
      s.autoUseHpItems = true;
      s.autoUseMpItems = true;
      s.autoUseStaminaItems = true;
      s.autoHpThresholdPercent = (byte) (45 + rnd.nextInt(26));
      s.autoMpThresholdPercent = (byte) (25 + rnd.nextInt(26));
      s.autoPickupItems = rnd.nextInt(100) < 85;
      s.autoRevive = true;
      s.autoAvoidEliteMobs = rnd.nextInt(100) < 40;
      s.autoIgnorePlayersAndBosses = true;
      s.autoAttackRange = 500 + rnd.nextInt(9) * 100; // treo offline: cắm 1 khúc bãi
      return s;
   }

   // ------------------------------------------------------------------ điểm tiềm năng + kỹ năng

   /** Tổng điểm tiềm năng nhận được từ cấp 1 tới {@code level}. */
   static long totalPoints(int level) {
      long total = 0;
      for (int l = 2; l <= level; l++) {
         total += com.tlnt.server.player.Leveling.talentPointsAt(l);
      }
      return total;
   }

   /**
    * Kỹ năng class nên nâng tiếp: cấp thấp nhất (rẻ nhất khi bằng cấp) còn nâng được ở cấp nhân vật hiện tại, giá ≤
    * {@code budget}. null = không còn gì nâng.
    */
   static SkillData nextSkill(PlayerData d, long budget) {
      SkillData best = null;
      int bestCost = 0;
      for (SkillData s : d.skills) {
         if (s.skillId == ClassSkills.BASIC_ATTACK) {
            continue;
         }
         SkillPack.Skill t = GameDataCache.skillTemplate(s.skillId);
         SkillPack.SkillLevel next = t == null ? null : SkillService.levelData(t, s.level + 1);
         if (next == null || next.requiredLevel() > d.level || next.upgradeCost() > budget) {
            continue;
         }
         if (best == null || s.level < best.level || (s.level == best.level && next.upgradeCost() < bestCost)) {
            best = s;
            bestCost = next.upgradeCost();
         }
      }
      return best;
   }

   /** Giá nâng {@code s} lên cấp kế (0 = không có cấp kế). */
   static int upgradeCost(SkillData s) {
      SkillPack.Skill t = GameDataCache.skillTemplate(s.skillId);
      SkillPack.SkillLevel next = t == null ? null : SkillService.levelData(t, s.level + 1);
      return next == null ? 0 : next.upgradeCost();
   }

   /** Tổng điểm đã tiêu cho kỹ năng (theo bảng giá) — so với {@link Build#skillShare} khi lên cấp. */
   static long spentOnSkills(PlayerData d) {
      long total = 0;
      for (SkillData s : d.skills) {
         SkillPack.Skill t = GameDataCache.skillTemplate(s.skillId);
         if (t == null || s.skillId == ClassSkills.BASIC_ATTACK) {
            continue;
         }
         for (int lv = 1; lv <= s.level; lv++) {
            SkillPack.SkillLevel l = SkillService.levelData(t, lv);
            total += l == null ? 0 : l.upgradeCost();
         }
      }
      return total;
   }

   /** Tiềm năng (id 1..6) đang thiếu nhất so với tỉ lệ của build; 0 = không có. */
   static int nextTalent(PlayerData d, Build build) {
      List<SkillPack.Talent> talents = GameDataCache.skill().talents();
      long spent = 0;
      for (SkillPack.Talent t : talents) {
         int i = t.id() - 1;
         if (i >= 0 && i < d.talents.length) {
            spent += (long) d.talents[i] * Math.max(1, t.pointCost());
         }
      }
      int best = 0;
      double bestGap = 0;
      for (SkillPack.Talent t : talents) {
         int i = t.id() - 1;
         if (i < 0 || i >= d.talents.length || i >= build.talents().length || build.talents()[i] <= 0) {
            continue;
         }
         double gap = build.talents()[i] - (spent == 0 ? 0 : (double) d.talents[i] * Math.max(1, t.pointCost()) / spent);
         if (best == 0 || gap > bestGap) {
            best = t.id();
            bestGap = gap;
         }
      }
      return best;
   }

   private static void spendPoints(PlayerData d, Build build, Random rnd) {
      long total = totalPoints(d.level);
      long skillBudget = (long) (total * (build.skillShare() - 0.05 + rnd.nextDouble() * 0.1));
      long left = total;
      while (true) {
         SkillData s = nextSkill(d, skillBudget);
         if (s == null) {
            break;
         }
         int cost = upgradeCost(s);
         s.level++;
         skillBudget -= cost;
         left -= cost;
      }
      List<SkillPack.Talent> talents = GameDataCache.skill().talents();
      int[] caps = GameDataCache.skill().talentPointCaps().isEmpty() ? null
            : GameDataCache.skill().talentPointCaps().get(0).caps();
      long budget = Math.max(0, left - rnd.nextInt(40)); // để thừa vài điểm
      for (SkillPack.Talent t : talents) {
         int i = t.id() - 1;
         if (i < 0 || i >= d.talents.length || i >= build.talents().length || t.pointCost() <= 0) {
            continue;
         }
         long points = (long) (budget * build.talents()[i]) / t.pointCost();
         if (caps != null && i < caps.length) {
            points = Math.min(points, caps[i]);
         }
         d.talents[i] = (int) points;
         left -= points * t.pointCost();
      }
      d.talentPoints = (int) Math.max(0, Math.min(Integer.MAX_VALUE, left));
   }

   /** Thanh kỹ năng: các chiêu class đã học (cấp ≥ 1) theo thứ tự bảng, tối đa 6 ô; chưa class = Đánh Thường. */
   private static void skillBar(PlayerData d) {
      int[] bar = new int[ClassSkills.BAR_SLOTS];
      java.util.Arrays.fill(bar, ClassSkills.EMPTY);
      int n = 0;
      for (SkillData s : d.skills) {
         if (n < bar.length && s.level >= 1 && (s.skillId != ClassSkills.BASIC_ATTACK || d.classId == WeaponClass.NONE)) {
            bar[n++] = s.skillId;
         }
      }
      d.skillBar = bar;
      ClassSkills.normalizeBar(d);
   }

   // ------------------------------------------------------------------ trang bị

   private static void gear(PlayerData d, Random rnd, long now) {
      Catalog c = catalog();
      int maxStars = BotProgress.maxStars(d.level);
      byte grade = (byte) (1 + rnd.nextInt(2)); // hệ đá quen dùng: Nhật ấn / Nguyệt ấn
      for (Map.Entry<Integer, List<ItemPack.ItemTemplate>> e : c.armorBySlot().entrySet()) {
         int slot = e.getKey();
         boolean minor = slot >= 5; // đai / mũ / vòng cổ: người cấp thấp hay thiếu
         if (rnd.nextInt(100) < (minor && d.level < 20 ? 25 : 5)) {
            continue;
         }
         List<ItemPack.ItemTemplate> fits = new ArrayList<>();
         for (ItemPack.ItemTemplate t : e.getValue()) {
            if (t.requiredLevel() <= d.level && (t.classId() == 0 || t.classId() == d.gender)
                  && ShopService.goods(t.id()) != null) { // chỉ đồ có bán ở shop NPC
               fits.add(t);
            }
         }
         if (fits.isEmpty()) {
            continue;
         }
         fits.sort(Comparator.comparingInt(ItemPack.ItemTemplate::requiredLevel).reversed());
         int pick = fits.size() > 1 && rnd.nextInt(100) < 25 ? 1 : 0; // đôi khi chưa thay món mới
         d.body.set(slot, equip(fits.get(pick).id(), maxStars, grade, rnd, now));
      }
      List<Integer> weapons = c.weaponsByClass().get(d.classId);
      if (weapons != null && !weapons.isEmpty()) {
         d.body.set(Appearance.SLOT_WEAPON,
               equip(weapons.get(Math.min(weaponTier(d.level, rnd), weapons.size() - 1)), maxStars, grade, rnd, now));
      }
   }

   /** Bậc vũ khí theo cấp: 0 Đá, 1 Bạc (25+), 2 Vàng (45+). */
   static int weaponTier(int level, Random rnd) {
      int r = rnd.nextInt(100);
      if (level >= 45) {
         return r < 70 ? 2 : 1;
      }
      if (level >= 25) {
         return r < 70 ? 1 : 0;
      }
      return 0;
   }

   /** Số sao 1 món: 0..{@code max}, lấy min 2 lần roll (đa số món sao thấp, ít món cao). */
   static int rollStars(int max, Random rnd) {
      return max <= 0 ? 0 : Math.min(rnd.nextInt(max + 1), rnd.nextInt(max + 1));
   }

   private static ItemData equip(int templateId, int maxStars, byte grade, Random rnd, long now) {
      ItemData item = create(ShopService.goods(templateId), templateId, now);
      item.starCount = (byte) Math.min(com.tlnt.server.forge.UpgradeRules.MAX_LEVEL, rollStars(maxStars, rnd));
      if (item.starCount > 0) {
         item.starGrade = rnd.nextInt(100) < 85 ? grade : (byte) (3 - grade);
      }
      return item;
   }

   private static ItemData create(RewardItem source, int templateId, long now) {
      ItemData item = source != null ? source.create(now) : ItemData.of(templateId, 1);
      item.quantity = 1;
      item.locked = true;
      return item;
   }

   // ------------------------------------------------------------------ túi + bộ sưu tập

   private static void bag(PlayerData d, Random rnd) {
      int tier = Math.max(0, Math.min(9, d.level / 12));
      int[][] stacks = {{RED_APPLE + tier, 25 + rnd.nextInt(60)}, {GREEN_APPLE + tier, 15 + rnd.nextInt(45)},
            {DRUMSTICK, 5 + rnd.nextInt(15)}, {ItemService.REVIVE_HERB_TEMPLATE_ID, 2 + rnd.nextInt(5)}};
      for (int i = 0; i < stacks.length; i++) {
         if (GameDataCache.itemTemplate(stacks[i][0]) == null) {
            continue;
         }
         ItemData item = ItemData.of(stacks[i][0], stacks[i][1]);
         item.locked = true;
         d.bag.set(i, item);
      }
      d.quickSlots = new int[] {0, 1, 2, -1}; // [HP, MP, thể lực, -]
   }

   /** Bot câu cá: cầm Cần câu (vũ khí cất vào túi) + mồi. */
   static void fishingKit(PlayerData d, Random rnd) {
      int rod = FishingService.config().rodItem();
      int bait = FishingService.config().baitItem();
      ItemData weapon = d.body.take(Appearance.SLOT_WEAPON);
      if (weapon != null) {
         d.bag.add(weapon);
      }
      ItemData r = ItemData.of(rod, 1);
      r.locked = true;
      d.body.set(Appearance.SLOT_WEAPON, r);
      ItemData b = ItemData.of(bait, 20 + rnd.nextInt(80));
      b.locked = true;
      d.bag.add(b);
   }

   /**
    * "Hơi gặp": mỗi loại bộ sưu tập có {@code cfg.*Chance}% (cấp thấp ít hơn — tân thủ chưa kịp kiếm), món + chỉ số lấy
    * từ danh sách trong {@code bot_profile}.
    */
   private static void collections(PlayerData d, BotCosmetics looks, BotConfig cfg, Random rnd, long now) {
      if (looks == null || cfg == null) {
         return;
      }
      int scale = d.level >= 30 ? 100 : d.level >= 15 ? 70 : d.level >= 8 ? 40 : 0; // % của tỉ lệ cấu hình
      if (roll(rnd, cfg.costumeChance(), scale)) {
         pickInto(d.costumes, looks.pick(BotCosmetics.COSTUME, rnd), now);
      }
      if (roll(rnd, cfg.titleChance(), scale)) {
         pickInto(d.titles, looks.pick(BotCosmetics.TITLE, rnd), now);
      }
      if (roll(rnd, cfg.petChance(), scale)) {
         pickInto(d.pets, looks.pick(BotCosmetics.PET, rnd), now);
      }
      if (roll(rnd, cfg.maskChance(), scale)) {
         pickInto(d.masks, looks.pick(BotCosmetics.MASK, rnd), now);
      }
      if (roll(rnd, cfg.backChance(), scale)) {
         pickInto(d.backs, looks.pick(BotCosmetics.BACK, rnd), now);
      }
   }

   private static boolean roll(Random rnd, int chance, int scalePercent) {
      return rnd.nextInt(10_000) < chance * scalePercent;
   }

   private static void pickInto(ItemCollection box, RewardItem source, long now) {
      if (source != null && GameDataCache.itemTemplate(source.templateId()) != null) {
         box.selected = box.add(create(source, source.templateId(), now));
      }
   }
}
