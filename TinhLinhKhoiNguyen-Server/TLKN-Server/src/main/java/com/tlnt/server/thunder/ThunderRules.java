package com.tlnt.server.thunder;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;

/**
 * Luật "Lôi vực Sấm sét" (chiến trường PK bộ lạc, map 31 / 32 / 33 — vào qua NPC 17 Tư tế Buraen). Bất biến, mọi luồng.
 *
 * <ul>
 *   <li>Map theo cấp: 5-25 -> 31 Tây, 26-45 -> 32 Bắc, 46+ -> 33 Đông. Mỗi map 1 khu, không đổi khu / map, không giới
 *       hạn số người ({@code map_template.max_players_per_zone} đặt rất lớn).</li>
 *   <li>Lượt / ngày: {@link #FREE_TURNS} miễn phí + mỗi Lệnh bài Đấu trường ({@link #TOKEN_ITEM}) "Sử dụng" được thêm
 *       1 lượt, tối đa {@link #MAX_TOKENS} lệnh bài / ngày. Đếm bằng {@code PlayerData.itemDaily} (cột item_daily, tự
 *       xoá sổ qua ngày): khoá {@link #TOKEN_ITEM} = lệnh bài đã dùng, khoá {@link #TURN_KEY} = lượt đã vào.</li>
 *   <li>1 lượt {@link #RUN_MS} (60 phút), hết giờ về làng. Rời map (Về làng, nút Thoát, bị loại, thoát game) = hết lượt.</li>
 *   <li>Vào map: cờ PK ép Bộ lạc (cờ tím, {@code PkRules.TRIBE}) — đánh mọi người trừ cùng bộ lạc, không đổi cờ được.</li>
 *   <li>Bị hạ gục: tự hồi sinh tại chỗ sau {@link #REVIVE_MS}; lần thứ {@link #MAX_DEATHS} + 1 bị loại về làng. Không dùng
 *       Cỏ hồi sinh.</li>
 *   <li>Vào lượt mới: +{@link #JOIN_DYNAMIC_POINTS} điểm hoạt động, +{@link #JOIN_TRIBE_POINTS} điểm cống hiến bộ lạc.
 *       Hạ gục người: +{@link #KILL_TRIBE_POINTS} cống hiến (tối đa {@link #KILL_TRIBE_CAP} / lượt) +
 *       {@link #KILL_DYNAMIC_POINTS} hoạt động (tối đa {@link #KILL_DYNAMIC_CAP} / lượt).</li>
 *   <li>Ấn linh (Tư tế trong map, {@link #SEAL_PRICE} lam thạch, 1 lần / lượt, chỉ có tác dụng trong Lôi vực): +5% Tấn
 *       công, +15% HP / MP tối đa, +15% Giáp ({@link #applySeal}).</li>
 * </ul>
 */
public final class ThunderRules {

   public static final int MAP_WEST = 31;
   public static final int MAP_NORTH = 32;
   public static final int MAP_EAST = 33;
   public static final int MIN_LEVEL = 5;
   /** Cấp cao nhất của map Tây / Bắc (cao hơn thì sang map sau). */
   static final int WEST_MAX_LEVEL = 25;
   static final int NORTH_MAX_LEVEL = 45;

   public static final long RUN_MS = 60 * 60_000L;
   public static final int FREE_TURNS = 2;
   /** Lệnh bài Đấu trường: "Sử dụng" = +1 lượt hôm nay. */
   public static final int TOKEN_ITEM = 293;
   public static final int MAX_TOKENS = 2;
   /** Khoá đếm lượt đã vào trong {@code PlayerData.itemDaily} (âm: không trùng id item thật). */
   public static final int TURN_KEY = -31;

   public static final long REVIVE_MS = 5_000;
   /** Bị hạ gục tối đa bấy nhiêu lần vẫn được hồi sinh; lần sau nữa bị loại. */
   public static final int MAX_DEATHS = 5;

   public static final int JOIN_DYNAMIC_POINTS = 10;
   public static final int JOIN_TRIBE_POINTS = 2;
   public static final int KILL_TRIBE_POINTS = 1;
   public static final int KILL_TRIBE_CAP = 5;
   public static final int KILL_DYNAMIC_POINTS = 2;
   public static final int KILL_DYNAMIC_CAP = 10;

   public static final String NO_REVIVE_HERB = "Không thể dùng Cỏ hồi sinh ở Lôi vực Sấm sét, bạn sẽ tự động hồi sinh"
         + " sau 5 giây.";
   public static final String NO_PK_SHIELD = "Không thể dùng Bùa hộ giáp ở Lôi vực Sấm sét.";

   public static final int SEAL_PRICE = 10;
   public static final int SEAL_ATTACK_PERCENT = 5;
   public static final int SEAL_HP_MP_PERCENT = 15;
   public static final int SEAL_ARMOR_PERCENT = 15;

   private ThunderRules() {
   }

   public static boolean isThunderMap(int mapId) {
      return mapId == MAP_WEST || mapId == MAP_NORTH || mapId == MAP_EAST;
   }

   /** Map của cấp {@code level}; 0 = chưa đủ cấp. */
   public static int mapFor(int level) {
      if (level < MIN_LEVEL) {
         return 0;
      }
      return level <= WEST_MAX_LEVEL ? MAP_WEST : level <= NORTH_MAX_LEVEL ? MAP_NORTH : MAP_EAST;
   }

   /** "phía tây" / "phía bắc" / "phía đông" của map. */
   static String side(int mapId) {
      return switch (mapId) {
         case MAP_NORTH -> "phía bắc";
         case MAP_EAST -> "phía đông";
         default -> "phía tây";
      };
   }

   /** Khoảng cấp của map ("5 - 25", "46 trở lên"). */
   static String levelRange(int mapId) {
      return switch (mapId) {
         case MAP_NORTH -> "từ cấp " + (WEST_MAX_LEVEL + 1) + " - " + NORTH_MAX_LEVEL;
         case MAP_EAST -> "từ cấp " + (NORTH_MAX_LEVEL + 1) + " trở lên";
         default -> "từ cấp " + MIN_LEVEL + " - " + WEST_MAX_LEVEL;
      };
   }

   /** Lượt còn lại hôm nay = miễn phí + lệnh bài đã dùng − lượt đã vào. */
   static int turnsLeft(int turnsUsed, int tokensUsed) {
      return Math.max(0, FREE_TURNS + tokensUsed - turnsUsed);
   }

   /** Còn dùng được Lệnh bài hôm nay (tối đa {@link #MAX_TOKENS} / ngày). */
   static boolean canUseToken(int tokensUsed) {
      return tokensUsed < MAX_TOKENS;
   }

   /** Điểm thưởng còn được nhận trong lượt: {@code each} mỗi lần, đã nhận {@code got}, tối đa {@code cap}. */
   static int capped(int each, int got, int cap) {
      return Math.max(0, Math.min(each, cap - got));
   }

   /** Ấn linh: chỉ khi đang đứng ở map Lôi vực trong lượt đã Ấn linh ({@code PlayerStats.of}). */
   public static void applySeal(PlayerStats s, PlayerData player) {
      ThunderRun run = player.thunderRun;
      if (run == null || !run.sealed || !isThunderMap(player.mapId) || run.over()) {
         return;
      }
      applySeal(s);
   }

   static void applySeal(PlayerStats s) {
      s.attackPower += s.attackPower * SEAL_ATTACK_PERCENT / 100;
      s.maxHp += s.maxHp * SEAL_HP_MP_PERCENT / 100;
      s.maxMp += s.maxMp * SEAL_HP_MP_PERCENT / 100;
      s.armor += s.armor * SEAL_ARMOR_PERCENT / 100;
   }
}
