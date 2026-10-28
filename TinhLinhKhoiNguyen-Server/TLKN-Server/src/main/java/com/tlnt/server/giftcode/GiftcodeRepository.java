package com.tlnt.server.giftcode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Giftcode: 1 bảng {@code tlnt_data.giftcode} (TLNT-Editor sửa, server tự nạp lại mỗi phút), quà là cột JSON
 * {@code rewards}:
 * <pre>
 * [{"i":38,"q":1,"l":1,"s":[[9,100]],"d":3,"c":0,"g":1}, ...]
 *  như quà phúc lợi (RewardItem) + c = chỉ loại vũ khí này nhận (0 = ai cũng nhận), g = chỉ nhân vật này nhận (0 = ai)
 * </pre>
 * Lượt đã nhập nằm trong dòng nhân vật (cột JSON {@code player.giftcodes}) — không có bảng riêng.
 */
public final class GiftcodeRepository {

   private static final Gson GSON = new Gson();
   private static final Type USES = new TypeToken<HashMap<String, Integer>>() { }.getType();

   private final Database dataDb;
   private final Database playerDb;

   public GiftcodeRepository(Database dataDb, Database playerDb) {
      this.dataDb = dataDb;
      this.playerDb = playerDb;
   }

   /** Mọi mã đang bật (enabled = 1), key = mã chữ hoa. null nếu đọc lỗi (giữ bản cũ). */
   public Map<String, Giftcode> loadCodes() {
      Map<String, Giftcode> codes = new HashMap<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT code, name, listed, start_at, end_at, min_level,"
                  + " max_level, class_ids, genders, player_names, max_uses, uses_per_player, rewards FROM giftcode"
                  + " WHERE enabled = 1");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            String code = normalize(rs.getString("code"));
            if (code.isEmpty()) {
               continue;
            }
            codes.put(code, new Giftcode(code, rs.getString("name"), rs.getBoolean("listed"),
                  millis(rs.getTimestamp("start_at")), millis(rs.getTimestamp("end_at")), rs.getInt("min_level"),
                  rs.getInt("max_level"), ints(rs.getString("class_ids")), ints(rs.getString("genders")),
                  names(rs.getString("player_names")), rs.getInt("max_uses"),
                  Math.max(0, rs.getInt("uses_per_player")), rewards(rs.getString("rewards"), code)));
         }
         return codes;
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang giftcode (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
   }

   /** Tổng lượt đã nhập mỗi mã cả server = cộng cột JSON {@code player.giftcodes} (1 lần lúc khởi động). */
   public Map<String, Integer> loadTotals() throws SQLException {
      Map<String, Integer> totals = new HashMap<>();
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT giftcodes FROM player WHERE giftcodes IS NOT NULL")) {
         ps.setFetchSize(1_000);
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               try {
                  Map<String, Integer> uses = GSON.fromJson(rs.getString(1), USES);
                  if (uses != null) {
                     uses.forEach((code, n) -> totals.merge(normalize(code), n == null ? 0 : n, Integer::sum));
                  }
               } catch (RuntimeException ignored) {
                  // JSON hỏng của 1 nhân vật -> bỏ qua
               }
            }
         }
      }
      return totals;
   }

   /** Mã người chơi gõ / trong SQL -> dạng so khớp: bỏ khoảng trắng 2 đầu, NFC, chữ hoa. */
   public static String normalize(String code) {
      return code == null ? "" : Normalizer.normalize(code.trim(), Normalizer.Form.NFC).toUpperCase(Locale.ROOT);
   }

   private static List<Giftcode.Reward> rewards(String json, String code) {
      List<Giftcode.Reward> rewards = new ArrayList<>();
      for (JsonObject o : RewardItem.objects(json, "giftcode " + code)) {
         RewardItem item = RewardItem.fromJson(o, true);
         if (item != null) {
            rewards.add(new Giftcode.Reward(o.has("c") ? o.get("c").getAsInt() : 0,
                  o.has("g") ? o.get("g").getAsInt() : 0, item));
         }
      }
      return List.copyOf(rewards);
   }

   private static long millis(Timestamp t) {
      return t == null ? 0 : t.getTime();
   }

   /** CSV "2,3" -> {2,3}; NULL / rỗng -> rỗng (= không giới hạn). */
   private static Set<Integer> ints(String csv) {
      Set<Integer> set = new HashSet<>();
      if (csv != null) {
         for (String part : csv.split("[,;\\s]+")) {
            if (!part.isBlank()) {
               try {
                  set.add(Integer.parseInt(part.trim()));
               } catch (NumberFormatException ignored) {
                  Log.warn("giftcode: '%s' khong phai so", part);
               }
            }
         }
      }
      return Set.copyOf(set);
   }

   private static Set<String> names(String csv) {
      Set<String> set = new HashSet<>();
      if (csv != null) {
         for (String part : csv.split("[,;\\s]+")) {
            if (!part.isBlank()) {
               set.add(Normalizer.normalize(part.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT));
            }
         }
      }
      return Set.copyOf(set);
   }
}
