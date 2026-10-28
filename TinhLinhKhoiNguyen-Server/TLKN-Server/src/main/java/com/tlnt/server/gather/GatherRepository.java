package com.tlnt.server.gather;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cột JSON {@code tlnt_data.mob_template.gather} -> {@link GatherConfig} (định dạng xem ở đó). */
public final class GatherRepository {

   static final java.util.Set<String> KEYS = java.util.Set.of("total", "perZone", "respawn", "level", "hp", "sting",
         "unlock", "items", "spots");

   private final Database dataDb;

   public GatherRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** null nếu chưa có cột / không dòng nào / đọc lỗi (giữ bản cũ). */
   public GatherConfig load() {
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "SELECT id, gather FROM mob_template WHERE gather IS NOT NULL AND gather <> ''");
            ResultSet rs = ps.executeQuery()) {
         List<GatherConfig.Entry> entries = new ArrayList<>();
         while (rs.next()) {
            GatherConfig.Entry entry = parse(rs.getInt(1), rs.getString(2));
            if (entry != null) {
               entries.add(entry);
            }
         }
         if (entries.isEmpty()) {
            Log.warn("mob_template.gather trong -> dung cau hinh to ong / dua mac dinh");
            return null;
         }
         return GatherConfig.of(entries);
      } catch (SQLException | RuntimeException e) {
         Log.warn("Khong doc duoc mob_template.gather (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
   }

   static GatherConfig.Entry parse(int mobId, String json) {
      try {
         JsonElement root = JsonParser.parseString(json);
         if (!root.isJsonObject()) {
            return null;
         }
         JsonObject o = root.getAsJsonObject();
         for (String key : o.keySet()) { // gõ sai tên khoá thì Gson vẫn đọc được -> báo để biết
            if (!KEYS.contains(key)) {
               Log.warn("mob_template.gather #%d: khoa la '%s' (bo qua) — dung: %s", mobId, key, KEYS);
            }
         }
         Map<Integer, List<float[]>> spots = new HashMap<>();
         if (o.has("spots") && o.get("spots").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("spots").entrySet()) {
               List<float[]> list = new ArrayList<>();
               if (e.getValue().isJsonArray()) {
                  for (JsonElement p : e.getValue().getAsJsonArray()) {
                     JsonArray a = p.getAsJsonArray();
                     list.add(new float[] {a.get(0).getAsFloat(), a.get(1).getAsFloat()});
                  }
               }
               spots.put(Integer.parseInt(e.getKey().trim()), List.copyOf(list));
            }
         }
         List<RewardItem> items = RewardItem.list(o.has("items") ? o.get("items").toString() : null, true,
               "mob_template.gather #" + mobId);
         return new GatherConfig.Entry(mobId, num(o, "total", 0), Math.max(1, num(o, "perZone", 1)),
               Math.max(1, num(o, "respawn", 300)), Math.max(1, num(o, "level", 1)), Math.max(1, num(o, "hp", 10)),
               Math.max(0, num(o, "sting", 0)), Math.max(0, Math.min(100, num(o, "unlock", 0))), List.copyOf(items),
               Map.copyOf(spots));
      } catch (RuntimeException e) {
         Log.warn("mob_template.gather #%d: JSON sai -> bo qua (%s)", mobId, e.getMessage());
         return null;
      }
   }

   private static int num(JsonObject o, String key, int fallback) {
      return o.has(key) ? o.get(key).getAsInt() : fallback;
   }
}
