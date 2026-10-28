package com.tlnt.server.forge;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hiệu ứng bộ Nhật ấn / Nguyệt ấn = {@code title} (bảng tlnt_data.title, client vẽ icon {@code icon} + hào quang {@code aura_animation} sun / moon +
 * tên + dòng mô tả ở bảng nhân vật; id gửi trong khối chỉ số — {@code PlayerData.titleId}).
 *
 * <pre>
 * Mặc đủ 8 món (ô 0..7) CÙNG hệ, món thấp nhất đạt:  3+ -> I   5+ -> II   7+ -> III   10 -> IV
 * Nhật ấn: title 1..4     Nguyệt ấn: title 5..8
 * </pre>
 * Chỉ số của title lấy từ chính các dòng mô tả (vd "+3% Tấn công", "+1,000 Chí mạng") khớp định dạng {@code stat_type}
 * -> sửa mô tả trong SQL là đổi luôn chỉ số.
 */
public final class SetTitles {

   public static final int SET_PIECES = 8;
   private static final int[] TIER_LEVELS = {3, 5, 7, 10};
   private static final int SUN_FIRST_TITLE = 1;
   private static final int MOON_FIRST_TITLE = 5;

   /** title id -> [[stat_type.id, giá trị], ...] (dựng lại khi dữ liệu nạp lại). */
   private record Parsed(GamePack game, Map<Integer, int[][]> stats) {
   }

   private static volatile Parsed parsed;

   private SetTitles() {
   }

   /** Title bộ đang kích hoạt theo đồ đang mặc, -1 = không. */
   public static int compute(PlayerData d) {
      byte grade = UpgradeRules.GRADE_NONE;
      int minLevel = Integer.MAX_VALUE;
      for (int slot = 0; slot < SET_PIECES; slot++) {
         ItemData item = d.body.get(slot);
         if (item == null || item.starGrade == UpgradeRules.GRADE_NONE) {
            return -1;
         }
         if (grade == UpgradeRules.GRADE_NONE) {
            grade = item.starGrade;
         } else if (item.starGrade != grade) {
            return -1; // lẫn Nhật / Nguyệt
         }
         minLevel = Math.min(minLevel, item.starCount);
      }
      int tier = -1;
      for (int i = 0; i < TIER_LEVELS.length; i++) {
         if (minLevel >= TIER_LEVELS[i]) {
            tier = i;
         }
      }
      if (tier < 0) {
         return -1;
      }
      return (grade == UpgradeRules.GRADE_SUN ? SUN_FIRST_TITLE : MOON_FIRST_TITLE) + tier;
   }

   /** Cộng chỉ số của title {@code titleId} vào {@code sum} (theo stat_type.id). */
   public static void addStats(long[] sum, int titleId) {
      if (titleId < 0) {
         return;
      }
      int[][] lines = parsed().stats().get(titleId);
      if (lines == null) {
         return;
      }
      for (int[] line : lines) {
         if (line[0] > 0 && line[0] < sum.length) {
            sum[line[0]] += line[1];
         }
      }
   }

   private static Parsed parsed() {
      GamePack game = GameDataCache.game();
      Parsed p = parsed;
      if (p == null || p.game() != game) {
         p = new Parsed(game, parseAll(game));
         parsed = p;
      }
      return p;
   }

   private static Map<Integer, int[][]> parseAll(GamePack game) {
      Map<Integer, int[][]> out = new HashMap<>();
      ItemPack items = GameDataCache.item();
      if (game == null || items == null) {
         return out;
      }
      List<Pattern> patterns = new ArrayList<>();
      List<Integer> ids = new ArrayList<>();
      for (ItemPack.StatType t : items.statTypes()) {
         String f = t.formatPattern();
         if (f == null || !f.contains("{0}")) {
            continue;
         }
         String[] parts = f.split("\\{0\\}", -1);
         patterns.add(Pattern.compile(Pattern.quote(parts[0].trim()) + "\\s*([0-9][0-9.,]*)\\s*"
               + Pattern.quote(parts.length > 1 ? parts[1].trim() : "")));
         ids.add(t.id());
      }
      for (GamePack.Title title : game.titles()) {
         List<int[]> lines = new ArrayList<>();
         for (String line : title.descriptionLines()) {
            String text = line == null ? "" : line.trim();
            for (int i = 0; i < patterns.size(); i++) {
               Matcher m = patterns.get(i).matcher(text);
               if (m.matches()) {
                  lines.add(new int[] {ids.get(i), Integer.parseInt(m.group(1).replace(",", "").replace(".", ""))});
                  break;
               }
            }
         }
         out.put(title.id(), lines.toArray(new int[0][]));
      }
      return out;
   }
}
