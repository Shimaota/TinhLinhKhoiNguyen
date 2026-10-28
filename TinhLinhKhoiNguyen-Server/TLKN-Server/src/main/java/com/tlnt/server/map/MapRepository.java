package com.tlnt.server.map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tlnt.server.db.Database;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc toàn bộ map từ SQL: 1 dòng {@code map_template} / map (va chạm, cửa, trang trí, NPC / quái / cây / vùng nước lưu
 * JSON).
 */
public final class MapRepository {

   private static final Gson GSON = new Gson();
   private static final Type WAYPOINT_LIST = new TypeToken<List<MapTemplate.Waypoint>>() { }.getType();
   private static final Type OBJECT_LIST = new TypeToken<List<MapTemplate.MapObject>>() { }.getType();
   private static final Type COLLIDER_LIST = new TypeToken<List<float[]>>() { }.getType();
   private static final Type NPC_LIST = new TypeToken<List<MapTemplate.NpcSpawn>>() { }.getType();
   private static final Type MOB_LIST = new TypeToken<List<MapTemplate.MobSpawn>>() { }.getType();
   private static final Type PLANT_LIST = new TypeToken<List<MapTemplate.PlantSpawn>>() { }.getType();
   private static final Type WATER_LIST = new TypeToken<List<MapTemplate.Water>>() { }.getType();

   private final Database db;

   public MapRepository(Database db) {
      this.db = db;
   }

   public List<MapTemplate> loadAll() throws SQLException {
      try (Connection conn = db.getConnection()) {
         boolean hasWaters = hasColumn(conn, "waters"); // cột tuỳ chọn (vùng nước vẽ thêm), nước chính tự nhận từ objects
         try (PreparedStatement ps = conn.prepareStatement("SELECT id, name, music, tile_cols, tile_rows, tile_size,"
                     + " width, height, spawn_x, spawn_y, max_players_per_zone, zone_count, waypoints, objects, colliders,"
                     + " npcs, mobs, plants" + (hasWaters ? ", waters" : "") + " FROM map_template ORDER BY id");
               ResultSet rs = ps.executeQuery()) {
            List<MapTemplate> maps = new ArrayList<>();
            while (rs.next()) {
               int id = rs.getInt(1);
               maps.add(new MapTemplate(id, rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5),
                     rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getFloat(9), rs.getFloat(10), rs.getInt(11),
                     Math.max(1, rs.getInt(12)), parseColliders(rs.getString(15), id),
                     parseList(rs.getString(13), WAYPOINT_LIST, id, "waypoints"),
                     parseList(rs.getString(14), OBJECT_LIST, id, "objects"),
                     parseList(rs.getString(16), NPC_LIST, id, "npcs"),
                     parseList(rs.getString(17), MOB_LIST, id, "mobs"),
                     parseList(rs.getString(18), PLANT_LIST, id, "plants"),
                     hasWaters ? parseList(rs.getString(19), WATER_LIST, id, "waters") : List.of()));
            }
            return maps;
         }
      }
   }

   private static boolean hasColumn(Connection conn, String column) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS"
            + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'map_template' AND COLUMN_NAME = ?")) {
         ps.setString(1, column);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
         }
      }
   }

   /**
    * Cột {@code colliders}: hộp {@code [x,y,w,h]} (phần tử thứ 5 = 1: nền 1 chiều) hoặc tam giác
    * {@code [x1,y1,x2,y2,x3,y3]} (phần tử thứ 7 = 1: 1 chiều).
    */
   static List<MapTemplate.Collider> parseColliders(String json, int mapId) {
      List<float[]> rows = parseList(json, COLLIDER_LIST, mapId, "colliders");
      List<MapTemplate.Collider> colliders = new ArrayList<>(rows.size());
      for (float[] r : rows) {
         if (r != null && r.length >= 6) {
            float[] pts = java.util.Arrays.copyOf(r, 6);
            float minX = Math.min(pts[0], Math.min(pts[2], pts[4]));
            float maxX = Math.max(pts[0], Math.max(pts[2], pts[4]));
            float minY = Math.min(pts[1], Math.min(pts[3], pts[5]));
            float maxY = Math.max(pts[1], Math.max(pts[3], pts[5]));
            colliders.add(new MapTemplate.Collider(minX, minY, maxX - minX, maxY - minY, r.length > 6 && r[6] != 0, pts));
            continue;
         }
         if (r == null || r.length < 4) {
            throw new IllegalStateException("JSON sai o map_template.id=" + mapId + " cot colliders: can [x,y,w,h]");
         }
         colliders.add(new MapTemplate.Collider(r[0], r[1], r[2], r[3], r.length > 4 && r[4] != 0));
      }
      return List.copyOf(colliders);
   }

   /** Cột {@code objects} (test / công cụ). */
   static List<MapTemplate.MapObject> parseObjects(String json, int mapId) {
      return parseList(json, OBJECT_LIST, mapId, "objects");
   }

   /** Cột {@code npcs} / {@code mobs} / {@code plants} (test / công cụ). */
   static List<MapTemplate.NpcSpawn> parseNpcs(String json, int mapId) {
      return parseList(json, NPC_LIST, mapId, "npcs");
   }

   static List<MapTemplate.MobSpawn> parseMobs(String json, int mapId) {
      return parseList(json, MOB_LIST, mapId, "mobs");
   }

   static List<MapTemplate.PlantSpawn> parsePlants(String json, int mapId) {
      return parseList(json, PLANT_LIST, mapId, "plants");
   }

   static List<MapTemplate.Water> parseWaters(String json, int mapId) {
      return parseList(json, WATER_LIST, mapId, "waters");
   }

   private static <T> List<T> parseList(String json, Type type, int mapId, String column) {
      if (json == null || json.isBlank()) {
         return List.of();
      }
      try {
         List<T> list = GSON.fromJson(json, type);
         return list == null ? List.of() : List.copyOf(list);
      } catch (RuntimeException e) {
         throw new IllegalStateException("JSON sai o map_template.id=" + mapId + " cot " + column, e);
      }
   }
}
