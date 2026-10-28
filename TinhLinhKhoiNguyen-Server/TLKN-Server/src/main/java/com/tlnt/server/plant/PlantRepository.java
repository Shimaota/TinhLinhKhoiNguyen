package com.tlnt.server.plant;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cột JSON {@code tlnt_data.plant_template.harvest} -> {@link PlantConfig} (định dạng xem ở đó). */
public final class PlantRepository {

   static final Set<String> KEYS = Set.of("maps", "max", "total", "respawn", "time", "items", "event");

   private final Database dataDb;

   public PlantRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** null nếu chưa có cột / không dòng nào cấu hình / đọc lỗi (giữ bản cũ). */
   public PlantConfig load() {
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "SELECT id, harvest FROM plant_template WHERE harvest IS NOT NULL AND harvest <> ''");
            ResultSet rs = ps.executeQuery()) {
         List<PlantConfig.Entry> entries = new ArrayList<>();
         while (rs.next()) {
            PlantConfig.Entry entry = parse(rs.getInt(1), rs.getString(2));
            if (entry != null) {
               entries.add(entry);
            }
         }
         if (entries.isEmpty()) {
            Log.warn("plant_template.harvest trong -> dung cau hinh hai cay mac dinh");
            return null;
         }
         return PlantConfig.of(entries);
      } catch (SQLException | RuntimeException e) {
         Log.warn("Khong doc duoc plant_template.harvest (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
   }

   static PlantConfig.Entry parse(int templateId, String json) {
      try {
         JsonElement root = JsonParser.parseString(json);
         if (!root.isJsonObject()) {
            return null;
         }
         JsonObject o = root.getAsJsonObject();
         for (String key : o.keySet()) { // gõ sai tên khoá (vd thiếu dấu ") thì Gson vẫn đọc được -> báo để biết
            if (!KEYS.contains(key)) {
               Log.warn("plant_template.harvest #%d: khoa la '%s' (bo qua) — dung: %s", templateId, key, KEYS);
            }
         }
         if (o.has("event") && !com.tlnt.server.event.GameEvents.activeKey(o.get("event").getAsString())) {
            return null; // cây của sự kiện đang tắt (vd Cà rốt Trung thu): không mọc / không hái
         }
         Set<Integer> maps = new HashSet<>();
         if (o.has("maps") && o.get("maps").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("maps")) {
               maps.add(e.getAsInt());
            }
         }
         int min = 60;
         int max = 180;
         if (o.has("respawn") && o.get("respawn").isJsonArray()) {
            JsonArray r = o.getAsJsonArray("respawn");
            min = r.size() > 0 ? r.get(0).getAsInt() : min;
            max = r.size() > 1 ? r.get(1).getAsInt() : min;
         } else if (o.has("respawn")) {
            min = max = o.get("respawn").getAsInt();
         }
         List<RewardItem> items = RewardItem.list(o.has("items") ? o.get("items").toString() : null, false,
               "plant_template.harvest #" + templateId);
         return new PlantConfig.Entry(templateId, Set.copyOf(maps), o.has("max") ? Math.max(0, o.get("max").getAsInt()) : 0,
               o.has("total") ? Math.max(0, o.get("total").getAsInt()) : 0, min, max,
               o.has("time") ? Math.max(500, o.get("time").getAsInt()) : 3_000, List.copyOf(items));
      } catch (RuntimeException e) {
         Log.warn("plant_template.harvest #%d: JSON sai -> bo qua (%s)", templateId, e.getMessage());
         return null;
      }
   }
}
