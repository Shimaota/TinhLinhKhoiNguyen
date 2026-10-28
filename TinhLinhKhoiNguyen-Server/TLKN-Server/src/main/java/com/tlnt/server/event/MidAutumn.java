package com.tlnt.server.event;

import com.tlnt.server.item.GiftBoxes;
import com.tlnt.server.item.GiftBoxes.Box;
import com.tlnt.server.item.GiftBoxes.Entry;
import com.tlnt.server.item.GiftBoxes.Mode;
import com.tlnt.server.item.ItemService;

import java.util.List;
import java.util.Set;

/**
 * Hằng số + bảng quà sự kiện Trung thu (spec người dùng 2026-10-13). Bất biến.
 *
 * <pre>
 * NPC 21 Nguyệt linh thần (map 2)   Nhận quà Trung thu (điểm danh / ngày, xong NV 6) | Làm bánh | Cửa hàng (shop 8)
 * Bánh nướng 350 = 5 Bột mì 343 + 10 Đường 345 + 10 Sữa tươi 346 + 2 Trứng muối 347 + 1 Mật ong 151
 * Bánh dẻo 349   = 5 Bột nếp 344 + 10 Đường + 10 Sữa tươi + 2 Trứng muối + 1 Quả dừa 146
 * Dùng bánh +15 điểm, nhặt cà rốt (cây 11) +1, cho Ngọc thố ăn +3, thả đèn hoa đăng 354 +5, hạ Cổ thần Dạ xoa +25
 * Tinh anh rơi 1-2 / Thủ lĩnh 2-5 mỗi loại Bột mì, Bột nếp, Đường, Sữa tươi (không khoá)
 * </pre>
 * Trọng số = % trong spec (tổng không cần đúng 100). "10% ra, 1% vĩnh viễn" = trọng số 10, 10‰ vĩnh viễn.
 */
public final class MidAutumn {

   public static final int NPC = 21;
   public static final int SHOP = 8;
   public static final int CARROT_PLANT = 11;
   /** {@code tlnt_data.world_boss.id} Cổ thần Dạ xoa. */
   public static final int BOSS_ID = 11;
   /** Map có cà rốt / Ngọc thố / boss. */
   public static final List<Integer> MAPS = List.of(1, 2, 3, 4);
   /** Nhận quà điểm danh: phải XONG nhiệm vụ chính tuyến 6 "Ấn Ký Nhật Nguyệt". */
   public static final int CHECK_IN_QUEST = 6;

   // ---- item
   public static final int FLOUR = 343;
   public static final int RICE_FLOUR = 344;
   public static final int SUGAR = 345;
   public static final int MILK = 346;
   public static final int SALTED_EGG = 347;
   public static final int SNOW_CAKE = 349;
   public static final int BAKED_CAKE = 350;
   public static final int CARROT = 351;
   public static final int LANTERN_ITEM = 354;
   public static final int HONEY = 151;
   public static final int COCONUT = 146;
   static final int EXP_TUBE = 355;
   static final int ORIGIN_STONE = 274;
   static final int ELEMENT_BAG = 269;
   static final int TALENT_BOOK = 145;
   static final int QUEST_TICKET = 309;
   static final int PET_JADE_RABBIT = 332;
   static final int PET_BLOOD_WOLF = 333;
   static final int COSTUME_MOON_GOD = 337;
   static final int COSTUME_YAKSHA = 338;
   static final int MASK_BLOOD_MOON = 340;
   static final int MASK_GHOST_MOON = 341;
   static final int MASK_JADE_RABBIT = 342;
   static final int MOON_LANTERN = 352;

   // ---- điểm sự kiện
   public static final int POINTS_CAKE = 15;
   public static final int POINTS_CARROT_PICK = 1;
   public static final int POINTS_FEED = 3;
   public static final int POINTS_LANTERN = 5;
   public static final int POINTS_BOSS = 25;

   /** Công thức: {mẫu bánh, [mẫu, số lượng]...}. */
   public record Recipe(int cake, String name, int[][] needs) {
   }

   public static final Recipe BAKED = new Recipe(BAKED_CAKE, "Bánh nướng",
         new int[][] {{FLOUR, 5}, {SUGAR, 10}, {MILK, 10}, {SALTED_EGG, 2}, {HONEY, 1}});
   public static final Recipe SNOW = new Recipe(SNOW_CAKE, "Bánh dẻo",
         new int[][] {{RICE_FLOUR, 5}, {SUGAR, 10}, {MILK, 10}, {SALTED_EGG, 2}, {COCONUT, 1}});

   /** Nguyên liệu Tinh anh / Thủ lĩnh rơi (không khoá). */
   static final int[] ELITE_MATERIALS = {FLOUR, RICE_FLOUR, SUGAR, MILK};
   static final int[] ELITE_QTY = {1, 2};
   static final int[] LEADER_QTY = {2, 5};

   // ---- stat_type.id
   private static final int HP = 3;
   private static final int MP = 5;
   private static final int ATK = 7;
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
   private static final int AMPLIFY_PCT = 44;

   // ---- chỉ số pet / trang phục / mặt nạ (dùng chung cho bánh, quà điểm danh, đèn)
   static final int[][] JADE_RABBIT_STATS = {s(HP, 500, 1000), s(MP, 500, 1000), s(AMPLIFY_PCT, 1, 3)};
   static final int[][] BLOOD_WOLF_STATS = {s(PIERCE, 75, 150), s(ACCURACY, 200, 350), s(SPEED, 25, 50)};
   static final int[][] MOON_GOD_STATS = {s(MANASTEAL_PCT, 1, 5), s(ARMOR, 75, 150), s(DODGE, 200, 350)};
   static final int[][] YAKSHA_STATS = {s(LIFESTEAL_PCT, 1, 5), s(ATK, 35, 75), s(CRIT, 200, 350)};
   static final int[][] MASK_JADE_RABBIT_STATS = {s(MP, 500, 1000), s(ATK, 35, 75), s(WEAKEN_DMG, 50, 100)};
   static final int[][] MASK_BLOOD_MOON_STATS = {s(HP, 500, 1000), s(ARMOR, 100, 200), s(STUN_DMG, 50, 100)};
   static final int[][] MASK_GHOST_MOON_STATS = {s(CRIT_DMG_PCT, 1, 3), s(CRIT, 350, 500), s(ATK_MOB, 75, 150)};
   static final int[][] MOON_LANTERN_STATS = {s(ARMOR, 75, 150), s(ATK_MOB, 50, 100)};

   /** "Có tỷ lệ ra vĩnh viễn ... 1% ra vĩnh viễn" = 10‰ của số lần ra món đó. */
   private static final int PERM_PERMILLE = 10;

   /** Bánh dẻo: mỗi cái 1 món. Pet / mặt nạ / ngoại trang KHÔNG khoá, hạn 1-3 ngày. */
   static final Box SNOW_BOX = new Box(Mode.ONE, 0, 0, List.of(
         item(30, ORIGIN_STONE, 100, 200, true),
         item(25, ELEMENT_BAG, 1, 1, true),
         item(10, EXP_TUBE, 1, 1, true),
         timed(10, PET_JADE_RABBIT, JADE_RABBIT_STATS),
         timed(10, MASK_JADE_RABBIT, MASK_JADE_RABBIT_STATS),
         timed(10, COSTUME_MOON_GOD, MOON_GOD_STATS),
         item(2, 299, 1, 1, true), // Thuốc trí tuệ II
         item(2, 300, 1, 1, true), // Thuốc Sức mạnh II
         item(2, 301, 1, 1, true))); // Thuốc Cứng cáp II

   /** Bánh nướng. */
   static final Box BAKED_BOX = new Box(Mode.ONE, 0, 0, List.of(
         item(30, ORIGIN_STONE, 100, 200, true),
         item(25, ELEMENT_BAG, 1, 1, true),
         item(10, EXP_TUBE, 1, 1, true),
         timed(10, COSTUME_YAKSHA, YAKSHA_STATS),
         timed(10, PET_BLOOD_WOLF, BLOOD_WOLF_STATS),
         timed(10, MASK_BLOOD_MOON, MASK_BLOOD_MOON_STATS),
         item(2, 158, 1, 1, true), // Ma thuật Trường sinh
         item(2, 159, 1, 1, true), // Ma thuật Tinh thần
         item(2, 160, 1, 1, true), // Ma thuật Bạo kích
         item(2, 161, 1, 1, true), // Ma thuật Tốc độ
         item(2, 162, 1, 1, true), // Ma thuật Tập trung
         item(2, 163, 1, 1, true))); // Ma thuật Bất khuất

   /** Thả đèn hoa đăng: 1 món. */
   static final Box LANTERN_BOX = new Box(Mode.ONE, 0, 0, List.of(
         item(30, ItemService.GOLD_TEMPLATE_ID, 1_000, 10_000, true),
         item(20, ORIGIN_STONE, 100, 200, true),
         item(10, EXP_TUBE, 1, 1, true),
         new Entry(10, MOON_LANTERN, 1, 1, true, MOON_LANTERN_STATS, 1, 3, PERM_PERMILLE),
         new Entry(10, MASK_GHOST_MOON, 1, 1, true, MASK_GHOST_MOON_STATS, 1, 3, PERM_PERMILLE)));

   /** Quà điểm danh hằng ngày ở Nguyệt linh thần: 1 món ngẫu nhiên (đều nhau), mọi món khoá, pet / trang phục 1 ngày. */
   static final Box CHECK_IN_BOX = new Box(Mode.ONE, 0, 0, List.of(
         new Entry(1, PET_JADE_RABBIT, 1, 1, true, JADE_RABBIT_STATS, 1, 1, 0),
         new Entry(1, PET_BLOOD_WOLF, 1, 1, true, BLOOD_WOLF_STATS, 1, 1, 0),
         new Entry(1, COSTUME_MOON_GOD, 1, 1, true, MOON_GOD_STATS, 1, 1, 0),
         new Entry(1, COSTUME_YAKSHA, 1, 1, true, YAKSHA_STATS, 1, 1, 0),
         item(1, ORIGIN_STONE, 100, 100, true),
         item(1, TALENT_BOOK, 10, 10, true),
         item(1, ItemService.GOLD_TEMPLATE_ID, 25_000, 100_000, true),
         item(1, QUEST_TICKET, 1, 1, true),
         item(1, ELEMENT_BAG, 3, 5, true),
         item(1, COCONUT, 1, 1, true),
         item(1, HONEY, 1, 1, true)));

   /** Ngọc thố ăn 1 củ cà rốt: mỗi món 1 lần (đều có), Trứng muối / Sữa tươi 10% không khoá. */
   static final int RABBIT_UNLOCK_PERCENT = 10;
   static final int[] RABBIT_GOLD = {100, 5_000};

   /** Mẫu item dùng trong sự kiện (để kiểm tra dùng khi tắt sự kiện). */
   static final Set<Integer> EVENT_USE_ITEMS = Set.of(CARROT, LANTERN_ITEM);

   private MidAutumn() {
   }

   /** Đăng ký 2 loại bánh là hộp quà ({@code item/UsableItems.openBox}: túi đầy, "Dùng nhiều", bảng nhận quà). */
   static void registerBoxes() {
      GiftBoxes.register(SNOW_CAKE, SNOW_BOX);
      GiftBoxes.register(BAKED_CAKE, BAKED_BOX);
   }

   private static int[] s(int statTypeId, int min, int max) {
      return new int[] {statTypeId, min, max};
   }

   private static Entry item(int weight, int templateId, int min, int max, boolean locked) {
      return new Entry(weight, templateId, min, max, locked, new int[0][], 0, 0, 0);
   }

   /** Pet / mặt nạ / ngoại trang trong bánh: KHÔNG khoá, 1-3 ngày, 1% vĩnh viễn. */
   private static Entry timed(int weight, int templateId, int[][] stats) {
      return new Entry(weight, templateId, 1, 1, false, stats, 1, 3, PERM_PERMILLE);
   }
}
