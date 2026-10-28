package com.tlnt.server.item;

import com.tlnt.server.player.ItemData;
import com.tlnt.server.shop.ShopService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Hộp quà / rương / túi dùng từ hành trang (op 27 sub 2) -> ra vật phẩm. Bảng quà do người chơi đặt (2026-10-09), viết
 * thẳng trong code như {@code AltarRules}. Mở = {@link UsableItems#openBox}.
 *
 * <ul>
 *   <li>{@link Mode#ONE}: ra ĐÚNG 1 dòng theo TRỌNG SỐ ({@link Entry#weight} — con số "%" người chơi ghi, tổng không cần
 *       = 100: dòng 50 trong tổng 190 = 50/190).</li>
 *   <li>{@link Mode#ALL}: ra MỌI dòng ("Rương gỗ cấp N ra N ô").</li>
 *   <li>{@link Mode#GENDER_SET}: bộ trang bị theo giới tính nhân vật ({@link #STARTER_SETS}), chỉ số random giống
 *       shop NPC ({@link ShopService#goods}).</li>
 * </ul>
 * Option dạng khoảng random đều mỗi lần mở; món có hạn ({@link Entry#daysMin} > 0) random số ngày, riêng
 * {@link Entry#permPermille}‰ ra vĩnh viễn.
 */
public final class GiftBoxes {

   public enum Mode { ONE, ALL, GENDER_SET }

   /**
    * 1 dòng quà.
    *
    * @param weight trọng số khi {@link Mode#ONE} (bỏ qua với ALL)
    * @param locked khoá; {@code null} = theo khoá của chính hộp
    * @param stats option {@code {statTypeId, min, max}}
    * @param daysMin hạn dùng nhỏ nhất (ngày), 0 = vĩnh viễn
    * @param permPermille phần nghìn ra vĩnh viễn khi có hạn (5 = 0,5%)
    */
   public record Entry(int weight, int templateId, int qtyMin, int qtyMax, Boolean locked, int[][] stats, int daysMin,
         int daysMax, int permPermille) {
   }

   /**
    * 1 loại hộp.
    *
    * @param activityCost điểm hoạt động ({@code PlayerData.dynamicPoints}) trừ mỗi lần mở, 0 = không
    * @param dailyLimit số món mở được mỗi ngày, 0 = không giới hạn
    */
   public record Box(Mode mode, int activityCost, int dailyLimit, List<Entry> entries) {
   }

   /** Túi tân thủ: áo, quần, găng, giày, đai, băng theo {@code player.gender} (1 Rinn, 2 Lyria, 3 Bruk). */
   static final Map<Integer, int[]> STARTER_SETS = Map.of(
         1, new int[] {38, 39, 40, 41, 42, 43},
         2, new int[] {8, 9, 10, 11, 12, 13},
         3, new int[] {68, 69, 70, 71, 72, 73});

   // ---- stat_type.id dùng trong bảng
   private static final int HP = 3;
   private static final int HP_PCT = 4;
   private static final int MP = 5;
   private static final int MP_PCT = 6;
   private static final int ATK = 7;
   private static final int ATK_PCT = 8;
   private static final int ATK_MOB = 9;
   private static final int CRIT = 11;
   private static final int CRIT_DMG_PCT = 14;
   private static final int ARMOR = 15;
   private static final int PIERCE = 17;
   private static final int DODGE = 19;
   private static final int ACCURACY = 21;
   private static final int SPEED = 23;
   private static final int LIFESTEAL_PCT = 30;
   private static final int MANASTEAL_PCT = 32;
   private static final int STUN_DMG = 35;
   private static final int WEAKEN_DMG = 37;
   private static final int LIGHTNING_DMG = 41;
   private static final int AMPLIFY_PCT = 44;
   private static final int EXP_PCT = 49;
   private static final int BURN_DMG = 50;

   // ---- item_template.id
   private static final int GOLD = ItemService.GOLD_TEMPLATE_ID;
   private static final int DIAMOND = ItemService.DIAMOND_TEMPLATE_ID;
   static final int TELEPORT_SCROLL = 124;
   private static final int ORIGIN_STONE = 274;
   private static final int ELEMENT_BAG = 269;
   private static final int QUEST_TICKET = 309;
   private static final int LEADER_SEAL = 261;

   /** Linh hồn rồng / pet / mặt nạ trong Rương gỗ (cùng option ở mọi cấp rương). */
   private static final Entry ZYREN = timed(0, 318, 10, s(ARMOR, 75, 150), s(DODGE, 200, 350), s(MP, 1000, 1500));
   private static final Entry VAELOR = timed(0, 319, 5, s(ATK, 35, 75), s(CRIT, 200, 350), s(HP, 1000, 1500));
   private static final Entry ZARVOK = timed(0, 322, 5, s(ATK, 35, 75), s(ACCURACY, 200, 350), s(PIERCE, 75, 150));
   private static final Entry ALTAR_MASK = timed(0, 320, 5, s(ATK_MOB, 75, 150), s(HP, 1000, 1500), s(MP, 1000, 1500));
   private static final Entry DIAMOND_1 = item(0, DIAMOND, 1, 1, false);
   private static final Entry SCROLLS = item(0, TELEPORT_SCROLL, 1, 5, true);
   private static final Entry TICKET = item(0, QUEST_TICKET, 1, 1, true);

   static final Map<Integer, Box> BOXES = Map.ofEntries(
         // 268 Túi linh thạch: 1 mảnh vũ khí 177..186, khoá
         Map.entry(268, new Box(Mode.ONE, 0, 0, range(177, 186))),
         // 269 Túi đá nguyên tố
         Map.entry(269, one(0, 0,
               item(20, 127, 1, 1, true), item(40, 128, 1, 1, true), item(35, 129, 1, 1, true),
               item(20, 130, 1, 1, true), item(35, 131, 1, 1, true), item(50, 132, 1, 1, true),
               item(50, 133, 1, 1, true))),
         // 172 Rương Hoạt động: -50 điểm hoạt động
         Map.entry(172, one(50, 0,
               item(50, GOLD, 5_000, 10_000, true), item(40, ORIGIN_STONE, 100, 200, true),
               item(20, 156, 1, 3, true), item(20, 157, 1, 3, true), item(15, 234, 1, 3, true),
               item(10, TELEPORT_SCROLL, 1, 1, true), item(10, 153, 1, 1, true), item(10, 154, 1, 1, true),
               item(10, 155, 1, 1, true), item(5, 235, 1, 1, true))),
         // 288 Quả Linh Mộc: -20 điểm hoạt động
         Map.entry(288, one(20, 0,
               item(50, GOLD, 5_000, 10_000, true), item(40, ELEMENT_BAG, 1, 5, true), item(5, 263, 1, 1, true),
               timed(10, 260, 5, s(CRIT, 200, 350), s(ATK_MOB, 75, 150), s(LIFESTEAL_PCT, 1, 3)),
               timed(10, 289, 5, s(ATK, 35, 75), s(ARMOR, 75, 150), s(EXP_PCT, 1, 5)),
               timed(10, 250, 5, s(ARMOR, 75, 150), s(LIFESTEAL_PCT, 1, 3), s(MANASTEAL_PCT, 1, 3)))),
         // Thỏi vàng: 30 / ngày CHUNG cả 2 loại ({@link #DAILY_GROUP}), 1.000.000 vàng (292 không khoá, 361 khoá)
         Map.entry(292, new Box(Mode.ALL, 0, 30, List.of(item(0, GOLD, 1_000_000, 1_000_000, false)))),
         Map.entry(361, new Box(Mode.ALL, 0, 30, List.of(item(0, GOLD, 1_000_000, 1_000_000, true)))),
         // 298 Túi Khởi nguyên: 10 / ngày, 1000-2000 đá khởi nguyên khoá
         Map.entry(298, new Box(Mode.ALL, 0, 10, List.of(item(0, ORIGIN_STONE, 1_000, 2_000, true)))),
         // Rương gỗ cấp 1..7 (Tế đàn Thiên tích): -25 điểm hoạt động, ra mọi ô
         Map.entry(311, all(gold(10_000, 20_000), DIAMOND_1, item(0, ORIGIN_STONE, 100, 200, true))),
         Map.entry(312, all(gold(20_000, 30_000), DIAMOND_1, stones(), SCROLLS)),
         Map.entry(313, all(gold(30_000, 40_000), DIAMOND_1, stones(), SCROLLS, ZYREN)),
         Map.entry(314, all(gold(40_000, 50_000), DIAMOND_1, stones(), SCROLLS, ZYREN, TICKET)),
         Map.entry(315, all(gold(50_000, 80_000), DIAMOND_1, stones(), SCROLLS, ZYREN, TICKET, VAELOR)),
         Map.entry(316, all(gold(80_000, 100_000), DIAMOND_1, stones(), SCROLLS, ZYREN, TICKET, VAELOR, ZARVOK)),
         Map.entry(317, all(gold(80_000, 100_000), DIAMOND_1, stones(), SCROLLS, ZYREN, TICKET, VAELOR, ZARVOK,
               ALTAR_MASK, item(0, LEADER_SEAL, 1, 1, true))),
         // 323 Rương Ngoại trang: 1 trong 12 ngoại trang, khoá, hạn 3 ngày
         Map.entry(323, one(0, 0,
               costume(192, s(ATK_PCT, 1, 5), s(CRIT_DMG_PCT, 1, 5), s(AMPLIFY_PCT, 1, 3)), // Tinh linh Bầu trời
               costume(273, s(ATK_PCT, 1, 5), s(HP_PCT, 5, 10), s(MP_PCT, 5, 10)),          // Thủ lĩnh Gorun
               costume(240, s(BURN_DMG, 100, 100), s(ATK, 75, 75), s(ARMOR, 350, 350)),     // Tinh linh Ánh lửa
               costume(270, s(DODGE, 500, 500), s(AMPLIFY_PCT, 3, 3)),                      // Setdan
               costume(241, s(LIGHTNING_DMG, 100, 100), s(ATK_MOB, 100, 100), s(DODGE, 350, 350)), // Tinh linh Sấm sét
               costume(190, s(STUN_DMG, 100, 100), s(ATK, 50, 50), s(ARMOR, 350, 350)),     // Tinh linh Phong thạch
               costume(189, s(MP, 2000, 2000), s(ATK_MOB, 100, 100), s(MANASTEAL_PCT, 5, 5)), // Tinh linh Biển cả
               costume(187, s(MP, 2000, 2000), s(ATK_MOB, 100, 100), s(LIFESTEAL_PCT, 5, 5)), // Tinh linh Rừng xanh
               costume(188, s(WEAKEN_DMG, 100, 100), s(ATK, 50, 50), s(CRIT, 350, 350)),    // Tinh linh Đầm lầy
               costume(303, s(ATK, 50, 50), s(SPEED, 25, 25), s(CRIT, 350, 350)),           // Tarae
               costume(200, s(ARMOR, 100, 100), s(ACCURACY, 350, 350), s(HP, 1500, 1500)),  // Sariah
               costume(236, s(ATK_MOB, 100, 100), s(PIERCE, 100, 100), s(MP, 1500, 1500)))), // Rinna
         // 357 Túi tân thủ: bộ trang bị theo giới tính, chỉ số như shop
         Map.entry(357, new Box(Mode.GENDER_SET, 0, 0, List.of())));

   /** Mẫu dùng chung lượt / ngày với mẫu khác: 361 Thỏi vàng khoá đếm chung với 292 Thỏi vàng. */
   private static final Map<Integer, Integer> DAILY_GROUP = Map.of(361, 292);

   private GiftBoxes() {
   }

   /** Khoá đếm lượt / ngày ({@code PlayerData.itemDaily}) của mẫu hộp. */
   public static int dailyKey(int templateId) {
      return DAILY_GROUP.getOrDefault(templateId, templateId);
   }

   /** Hộp do hệ thống khác đăng ký lúc khởi động (bánh Trung thu — {@code event/MidAutumn}). */
   private static final Map<Integer, Box> EXTRA = new java.util.concurrent.ConcurrentHashMap<>();

   public static void register(int templateId, Box box) {
      EXTRA.put(templateId, box);
   }

   /** Bảng quà của mẫu item; null = không phải hộp. */
   public static Box box(int templateId) {
      Box box = BOXES.get(templateId);
      return box != null ? box : EXTRA.get(templateId);
   }

   /**
    * Tung quà 1 lần mở. {@code boxLocked} = khoá của hộp (dòng không ghi khoá thì theo hộp), {@code gender} cho Túi tân
    * thủ. Không đụng PlayerData.
    */
   public static List<ItemData> roll(Box box, boolean boxLocked, int gender, long now, RandomGenerator random) {
      List<ItemData> out = new ArrayList<>();
      switch (box.mode()) {
         case ALL -> box.entries().forEach(e -> out.add(create(e, boxLocked, now, random)));
         case ONE -> {
            Entry e = pick(box.entries(), random);
            if (e != null) {
               out.add(create(e, boxLocked, now, random));
            }
         }
         case GENDER_SET -> {
            for (int templateId : STARTER_SETS.getOrDefault(gender, new int[0])) {
               RewardItem goods = ShopService.goods(templateId); // chỉ số random giống hàng shop NPC
               ItemData item = goods != null ? goods.create(now) : ItemData.of(templateId, 1);
               item.quantity = 1;
               item.locked = true;
               item.createdTime = now;
               out.add(item);
            }
         }
      }
      return out;
   }

   /** Chọn 1 dòng theo trọng số. */
   static Entry pick(List<Entry> entries, RandomGenerator random) {
      int total = 0;
      for (Entry e : entries) {
         total += Math.max(0, e.weight());
      }
      if (total <= 0) {
         return null;
      }
      int r = random.nextInt(total);
      for (Entry e : entries) {
         r -= Math.max(0, e.weight());
         if (r < 0) {
            return e;
         }
      }
      return null;
   }

   static ItemData create(Entry e, boolean boxLocked, long now, RandomGenerator random) {
      ItemData item = ItemData.of(e.templateId(), between(e.qtyMin(), e.qtyMax(), random));
      item.locked = e.locked() != null ? e.locked() : boxLocked;
      item.createdTime = now;
      for (int[] stat : e.stats()) {
         item.stats.add(new ItemData.StatValue(stat[0], new int[] {between(stat[1], stat[2], random)}));
      }
      if (e.daysMin() > 0) {
         boolean permanent = e.permPermille() > 0 && random.nextInt(1000) < e.permPermille();
         item.expireTime = permanent ? ItemData.PERMANENT
               : now + between(e.daysMin(), e.daysMax(), random) * 86_400_000L;
      }
      return item;
   }

   private static int between(int min, int max, RandomGenerator random) {
      return max > min ? random.nextInt(min, max + 1) : min;
   }

   // ---- dựng bảng

   private static int[] s(int statTypeId, int min, int max) {
      return new int[] {statTypeId, min, max};
   }

   private static Entry item(int weight, int templateId, int qtyMin, int qtyMax, Boolean locked) {
      return new Entry(weight, templateId, qtyMin, qtyMax, locked, new int[0][], 0, 0, 0);
   }

   /** Ngoại trang / pet / mặt nạ khoá, hạn 1-3 ngày, {@code permPermille}‰ vĩnh viễn. */
   private static Entry timed(int weight, int templateId, int permPermille, int[]... stats) {
      return new Entry(weight, templateId, 1, 1, true, stats, 1, 3, permPermille);
   }

   /** Ngoại trang của Rương Ngoại trang: khoá, hạn đúng 3 ngày, cùng trọng số. */
   private static Entry costume(int templateId, int[]... stats) {
      return new Entry(1, templateId, 1, 1, true, stats, 3, 3, 0);
   }

   private static Entry gold(int min, int max) {
      return item(0, GOLD, min, max, false);
   }

   private static Entry stones() {
      return item(0, ORIGIN_STONE, 300, 400, true);
   }

   private static List<Entry> range(int fromTemplate, int toTemplate) {
      List<Entry> list = new ArrayList<>();
      for (int id = fromTemplate; id <= toTemplate; id++) {
         list.add(item(1, id, 1, 1, true));
      }
      return List.copyOf(list);
   }

   private static Box one(int activityCost, int dailyLimit, Entry... entries) {
      return new Box(Mode.ONE, activityCost, dailyLimit, List.of(entries));
   }

   /** Rương gỗ Tế đàn: -25 điểm hoạt động (mô tả item "Cần 25 điểm hoạt động để mở rương"). */
   private static Box all(Entry... entries) {
      return new Box(Mode.ALL, 25, 0, List.of(entries));
   }
}
