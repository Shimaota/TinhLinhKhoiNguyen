package com.tlnt.server.data.sql;

import com.google.gson.Gson;
import com.tlnt.server.data.model.CharBaseStat;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemAppearance;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.data.model.ServerEntry;
import com.tlnt.server.data.model.SkillEffect;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc 4 phần dữ liệu tĩnh (game/map/item/skill) từ DB {@code tlnt_data} để nạp lên {@code GameDataCache}.
 * Mảng nhỏ (stat_ratings, frame_paths, icon_paths...) lưu dạng JSON array.
 */
public final class GameDataRepository {

   private static final Gson GSON = new Gson();

   private final Database db;

   public GameDataRepository(Database db) {
      this.db = db;
   }

   // ------------------------------------------------------------------ data_version

   public int getVersion(String part) throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT version FROM data_version WHERE part = ?")) {
         ps.setString(1, part);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
         }
      }
   }

   // ------------------------------------------------------------------ game

   public GamePack loadGame() throws SQLException {
      try (Connection conn = db.getConnection()) {
         List<GamePack.CharClass> classes = query(conn, "SELECT id, name FROM char_class ORDER BY id",
               rs -> new GamePack.CharClass(rs.getInt(1), rs.getString(2)));
         List<GamePack.CharTemplate> templates = query(conn, "SELECT id, name, skeleton_path, avatar_icon_path,"
               + " info_table_json, stat_ratings, selectable FROM char_template ORDER BY id",
               rs -> new GamePack.CharTemplate(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                     rs.getString(5), GSON.fromJson(rs.getString(6), float[].class), rs.getBoolean(7)));
         List<GamePack.MarketFee> fees = query(conn, "SELECT idx, name, percent FROM market_fee ORDER BY idx",
               rs -> new GamePack.MarketFee(rs.getInt(1), rs.getString(2), rs.getInt(3)));
         List<GamePack.Animal> animals = query(conn, "SELECT id, growing_name, mature_name, skeleton_path,"
               + " skin_name, scale, grow_duration_ms FROM animal_type ORDER BY id",
               rs -> new GamePack.Animal(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                     rs.getString(5), rs.getFloat(6), rs.getInt(7)));
         List<GamePack.Plant> plants = query(conn, "SELECT id, name, offset_x, offset_y, skeleton_name, idle_animation,"
               + " skeleton_scale FROM plant_template ORDER BY id",
               rs -> new GamePack.Plant(rs.getInt(1), rs.getString(2), rs.getFloat(3), rs.getFloat(4),
                     rs.getString(5), rs.getString(6), rs.getFloat(7)));
         List<GamePack.Title> titles = query(conn,
               "SELECT id, name, icon, aura_animation, description_lines FROM title ORDER BY id",
               rs -> new GamePack.Title(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                     GSON.fromJson(rs.getString(5), String[].class)));
         List<GamePack.TribeEmblem> emblems = query(conn,
               "SELECT id, name, frame_paths, frame_rate FROM tribe_emblem ORDER BY id",
               rs -> new GamePack.TribeEmblem(rs.getInt(1), rs.getString(2),
                     GSON.fromJson(rs.getString(3), String[].class), rs.getInt(4)));
         List<GamePack.TribeSkill> tribeSkills = query(conn,
               "SELECT id, name, icon, description FROM tribe_skill ORDER BY id",
               rs -> new GamePack.TribeSkill(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));

         Map<Integer, List<GamePack.GuideTopic>> topicsByCategory = groupBy(conn,
               "SELECT category_idx, id, name, layout_json FROM guide_topic ORDER BY category_idx, idx",
               rs -> new GamePack.GuideTopic(rs.getInt(2), rs.getString(3), rs.getString(4)));
         List<GamePack.GuideCategory> categories = query(conn,
               "SELECT idx, id, name, icon_path FROM guide_category ORDER BY idx",
               rs -> new GamePack.GuideCategory(rs.getInt(2), rs.getString(3), rs.getString(4),
                     topicsByCategory.getOrDefault(rs.getInt(1), List.of())));

         return new GamePack(getVersion("game"), classes, templates, fees, animals, plants, titles, emblems,
               tribeSkills, categories);
      }
   }

   // ------------------------------------------------------------------ map

   public MapPack loadMap() throws SQLException {
      try (Connection conn = db.getConnection()) {
         Map<Integer, List<MapPack.Tile>> tilesBySet = groupBy(conn, "SELECT tileset_id, tile_id, tile_type, src_x,"
               + " src_y, anim_frames FROM tileset_tile ORDER BY tileset_id, idx",
               rs -> new MapPack.Tile(rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),
                     arrayToFrames(rs.getString(6))));
         List<MapPack.Tileset> tilesets = query(conn,
               "SELECT id, texture_name, tile_width, tile_height FROM tileset ORDER BY id",
               rs -> new MapPack.Tileset(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                     tilesBySet.getOrDefault(rs.getInt(1), List.of())));
         List<MapPack.Decoration> decorations = query(conn, "SELECT id, name, frames_per_second, anchor_id,"
               + " frame_names FROM map_decoration ORDER BY id",
               rs -> new MapPack.Decoration(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                     GSON.fromJson(rs.getString(5), String[].class)));
         List<MapPack.Particle> particles = query(conn, "SELECT idx, json FROM particle_effect ORDER BY idx",
               rs -> new MapPack.Particle(rs.getInt(1), rs.getString(2)));
         Map<Integer, List<MapPack.MobVariant>> variantsByMob = groupBy(conn, "SELECT mob_id, name, skin, speed,"
               + " show_health_bar FROM mob_variant ORDER BY mob_id, idx",
               rs -> new MapPack.MobVariant(rs.getString(2), rs.getString(3), rs.getFloat(4), rs.getBoolean(5)));
         List<MapPack.Mob> mobs = query(conn, "SELECT id, name, wander_type, wander_range, skeleton_path, scale"
               + " FROM mob_template ORDER BY id",
               rs -> new MapPack.Mob(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5),
                     rs.getFloat(6), variantsByMob.getOrDefault(rs.getInt(1), List.of())));
         List<MapPack.Npc> npcs = query(conn, "SELECT id, name, overhead_text, faces_player, talk_button, kind,"
               + " skeleton_path, idle_animation, scale, avatar_icon FROM npc_template ORDER BY id",
               rs -> new MapPack.Npc(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4),
                     rs.getBoolean(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getFloat(9),
                     rs.getObject(10, Integer.class)));
         Map<Integer, List<MapPack.MapMarker>> markersByPage = groupBy(conn, "SELECT page_id, map_id, name, x, y"
               + " FROM world_map_marker ORDER BY page_id, idx",
               rs -> new MapPack.MapMarker(rs.getInt(2), rs.getString(3), rs.getFloat(4), rs.getFloat(5)));
         List<MapPack.WorldMapPage> pages = query(conn,
               "SELECT id, name, map_image_path FROM world_map_page ORDER BY id",
               rs -> new MapPack.WorldMapPage(rs.getInt(1), rs.getString(2), rs.getString(3),
                     markersByPage.getOrDefault(rs.getInt(1), List.of())));

         List<MapPack.MapEffect> effects = query(conn, "SELECT id, name, path FROM map_effect ORDER BY idx",
               rs -> new MapPack.MapEffect(rs.getInt(1), rs.getString(2), rs.getString(3)));

         return new MapPack(getVersion("map"), tilesets, decorations, particles, mobs, npcs, pages, effects);
      }
   }

   // ------------------------------------------------------------------ item

   /** Bảng server-only {@code item_appearance}; chưa tạo bảng (DB cũ) thì trả rỗng + cảnh báo. */
   public List<ItemAppearance> loadItemAppearances() throws SQLException {
      try (Connection conn = db.getConnection()) {
         return query(conn,
               "SELECT item_template_id, char_template_id, skin_key, title_key FROM item_appearance",
               rs -> new ItemAppearance(rs.getInt(1), rs.getInt(2), emptyToNull(rs.getString(3)),
                     emptyToNull(rs.getString(4))));
      } catch (SQLException e) {
         if (e.getErrorCode() == 1146) { // ER_NO_SUCH_TABLE
            com.tlnt.server.util.Log.warn("Chua co bang item_appearance (chay sql/migrations) -> item khong doi ngoai hinh");
            return List.of();
         }
         throw e;
      }
   }

   // ------------------------------------------------------------------ skill

   /** Bảng server-only {@code skill_effect}; chưa tạo bảng (DB cũ) thì trả rỗng + cảnh báo (không đánh được). */
   public List<SkillEffect> loadSkillEffects() throws SQLException {
      try (Connection conn = db.getConnection()) {
         return query(conn, "SELECT skill_id, level, damage_percent, hits, aoe_targets, aoe_percent, armor_pen,"
               + " accuracy, crit_damage_percent, lifesteal_percent, bleed_percent, bonus_percent, bonus_stat, status,"
               + " status_power, status_ms, heal_percent, damage_reduction_percent, reflect_percent,"
               + " attack_speed_percent, move_speed_percent, illusion_percent, duration_ms FROM skill_effect",
               rs -> new SkillEffect(rs.getInt(1), rs.getInt(2), rs.getInt(3), Math.max(1, rs.getInt(4)),
                     rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10),
                     rs.getInt(11), rs.getInt(12), emptyToNull(rs.getString(13)), emptyToNull(rs.getString(14)),
                     rs.getInt(15), rs.getInt(16), rs.getInt(17), rs.getInt(18), rs.getInt(19), rs.getInt(20),
                     rs.getInt(21), rs.getInt(22), rs.getInt(23)));
      } catch (SQLException e) {
         if (e.getErrorCode() == 1146) { // ER_NO_SUCH_TABLE
            com.tlnt.server.util.Log.warn("Chua co bang skill_effect (chay sql/migrations) -> khong dung duoc ky nang");
            return List.of();
         }
         throw e;
      }
   }

   private static String emptyToNull(String value) {
      return value == null || value.isBlank() ? null : value.trim();
   }

   public ItemPack loadItem() throws SQLException {
      try (Connection conn = db.getConnection()) {
         List<ItemPack.ItemType> types = query(conn,
               "SELECT id, name, equip_slot, equippable FROM item_type ORDER BY id",
               rs -> new ItemPack.ItemType(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBoolean(4)));
         List<ItemPack.ItemTemplate> templates = query(conn, "SELECT id, type_id, class_id, name, description,"
               + " icon_path, preview_image_path, required_level, emblem_path FROM item_template ORDER BY id",
               rs -> new ItemPack.ItemTemplate(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4),
                     rs.getString(5), rs.getString(6), rs.getString(7), rs.getInt(8), rs.getString(9)));
         List<ItemPack.StatType> stats = query(conn, "SELECT id, format_pattern FROM stat_type ORDER BY id",
               rs -> new ItemPack.StatType(rs.getInt(1), rs.getString(2)));
         List<ItemPack.OptionInfo> options = query(conn,
               "SELECT id, name, icon, description FROM option_info ORDER BY id",
               rs -> new ItemPack.OptionInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));

         return new ItemPack(getVersion("item"), types, templates, stats, options);
      }
   }

   // ------------------------------------------------------------------ skill

   public SkillPack loadSkill() throws SQLException {
      try (Connection conn = db.getConnection()) {
         List<SkillPack.TalentPointCap> caps = query(conn,
               "SELECT id, required_level, caps FROM talent_point_cap ORDER BY id",
               rs -> new SkillPack.TalentPointCap(rs.getInt(1), rs.getInt(2),
                     GSON.fromJson(rs.getString(3), int[].class)));
         List<SkillPack.Talent> talents = query(conn,
               "SELECT id, name, icon, description, point_cost FROM talent ORDER BY id",
               rs -> new SkillPack.Talent(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                     rs.getInt(5)));
         Map<Integer, List<SkillPack.SkillLevel>> levelsBySkill = groupBy(conn, "SELECT skill_id, level, range_x,"
               + " range_y, mana_cost, cooldown_millis, required_level, upgrade_cost, gold_cost, exp_cost,"
               + " effect_description FROM skill_level ORDER BY skill_id, idx",
               rs -> new SkillPack.SkillLevel(rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6),
                     rs.getInt(7), rs.getInt(8), rs.getLong(9), rs.getLong(10), rs.getString(11)));
         List<SkillPack.Skill> skills = query(conn, "SELECT id, name, target_type, class_id, mana_cost_type,"
               + " description, icon_paths FROM skill_template ORDER BY id",
               rs -> new SkillPack.Skill(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),
                     rs.getString(6), GSON.fromJson(rs.getString(7), String[].class),
                     levelsBySkill.getOrDefault(rs.getInt(1), List.of())));

         return new SkillPack(getVersion("skill"), caps, talents, skills);
      }
   }

   // ------------------------------------------------------------------ server list

   public List<ServerEntry> loadServerList() throws SQLException {
      try (Connection conn = db.getConnection()) {
         return query(conn, "SELECT id, name, host, port FROM server_list WHERE enabled = 1 ORDER BY sort_order, id",
               rs -> new ServerEntry(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
      }
   }

   public List<CharBaseStat> loadCharBaseStats() throws SQLException {
      try (Connection conn = db.getConnection()) {
         return query(conn, "SELECT template_id, hp, mp, attack, attack_vs_mob, crit_damage_percent, armor, move_speed,"
                     + " hp_regen, mp_regen, damage_amp_percent, stamina"
                     + " FROM char_base_stat",
               rs -> new CharBaseStat(rs.getInt(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5),
                     rs.getLong(6), rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getLong(10), rs.getLong(11),
                     rs.getInt(12)));
      }
   }

   // ------------------------------------------------------------------ helpers

   @FunctionalInterface
   private interface RowMapper<T> {
      T map(ResultSet rs) throws SQLException;
   }

   private static <T> List<T> query(Connection conn, String sql, RowMapper<T> mapper) throws SQLException {
      try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
         List<T> out = new ArrayList<>();
         while (rs.next()) {
            out.add(mapper.map(rs));
         }
         return out;
      }
   }

   /** Nhóm dòng con theo cột 1 (khoá cha), giữ thứ tự ORDER BY. */
   private static <T> Map<Integer, List<T>> groupBy(Connection conn, String sql, RowMapper<T> mapper)
         throws SQLException {
      try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
         Map<Integer, List<T>> out = new HashMap<>();
         while (rs.next()) {
            out.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(mapper.map(rs));
         }
         return out;
      }
   }

   private static List<MapPack.AnimFrame> arrayToFrames(String json) {
      if (json == null) {
         return List.of();
      }
      int[][] raw = GSON.fromJson(json, int[][].class);
      List<MapPack.AnimFrame> out = new ArrayList<>(raw.length);
      for (int[] f : raw) {
         out.add(new MapPack.AnimFrame(f[0], f[1]));
      }
      return out;
   }
}
