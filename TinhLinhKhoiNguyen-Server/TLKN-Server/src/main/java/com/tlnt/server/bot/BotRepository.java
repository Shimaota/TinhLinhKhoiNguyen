package com.tlnt.server.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc / ghi {@code tlnt_data.bot_profile} (1 dòng id 1 — migration {@code sql/migrations/2026-10-17_bot_profile.sql}):
 * cài đặt ({@link BotConfig}), 5 danh sách bộ sưu tập bot được dùng (JSON {@link RewardItem} có chỉ số — ghi 1 món nhiều
 * lần = gặp nhiều hơn) và câu chat theo loại bot (JSON {"idle": [...], "grind": [...], ...}). SQL: luồng blocking / admin.
 */
final class BotRepository {

   static final int ROW = 1;

   /** Toàn bộ dòng: cài đặt + bộ sưu tập + câu chat. */
   record Profile(BotConfig config, BotCosmetics looks, Map<String, String[]> chat) {
   }

   private final Database dataDb;

   BotRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** null = chưa có bảng / dòng (chưa chạy migration) -> người gọi dùng mặc định. */
   Profile load() throws SQLException {
      try (Connection c = dataDb.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT * FROM bot_profile WHERE id = ?")) {
         ps.setInt(1, ROW);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               return null;
            }
            BotConfig cfg = new BotConfig(rs.getBoolean("enabled"), rs.getInt("bot_count"), rs.getInt("grind_percent"),
                  rs.getInt("auto_percent"), rs.getInt("fish_percent"), rs.getInt("afk_percent"),
                  rs.getInt("party_percent"), rs.getInt("boss_hunters"), rs.getInt("min_level"), rs.getInt("max_level"),
                  rs.getInt("zone_percent"), rs.getInt("session_min"), rs.getInt("session_max"), rs.getBoolean("chat"),
                  rs.getBoolean("pk_fight_back"), rs.getInt("spawn_per_second"), rs.getInt("costume_chance"),
                  rs.getInt("title_chance"), rs.getInt("pet_chance"), rs.getInt("mask_chance"), rs.getInt("back_chance"))
                  .normalized();
            Map<Integer, List<RewardItem>> looks = new HashMap<>();
            looks.put(BotCosmetics.COSTUME, items(rs.getString("costumes"), "bot_profile.costumes"));
            looks.put(BotCosmetics.TITLE, items(rs.getString("titles"), "bot_profile.titles"));
            looks.put(BotCosmetics.PET, items(rs.getString("pets"), "bot_profile.pets"));
            looks.put(BotCosmetics.MASK, items(rs.getString("masks"), "bot_profile.masks"));
            looks.put(BotCosmetics.BACK, items(rs.getString("backs"), "bot_profile.backs"));
            return new Profile(cfg, new BotCosmetics(looks), chat(rs.getString("chat_lines")));
         }
      }
   }

   /** Ghi phần cài đặt (giữ nguyên bộ sưu tập / câu chat). */
   void save(BotConfig c) throws SQLException {
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("UPDATE bot_profile SET enabled = ?, bot_count = ?,"
                  + " grind_percent = ?, auto_percent = ?, fish_percent = ?, afk_percent = ?, party_percent = ?,"
                  + " boss_hunters = ?, min_level = ?, max_level = ?, zone_percent = ?, session_min = ?, session_max = ?,"
                  + " chat = ?, pk_fight_back = ?, spawn_per_second = ?, costume_chance = ?, title_chance = ?,"
                  + " pet_chance = ?, mask_chance = ?, back_chance = ? WHERE id = ?")) {
         int i = 1;
         ps.setBoolean(i++, c.enabled());
         ps.setInt(i++, c.count());
         ps.setInt(i++, c.grindPercent());
         ps.setInt(i++, c.autoPercent());
         ps.setInt(i++, c.fishPercent());
         ps.setInt(i++, c.afkPercent());
         ps.setInt(i++, c.partyPercent());
         ps.setInt(i++, c.bossHunters());
         ps.setInt(i++, c.minLevel());
         ps.setInt(i++, c.maxLevel());
         ps.setInt(i++, c.zonePercent());
         ps.setInt(i++, c.sessionMin());
         ps.setInt(i++, c.sessionMax());
         ps.setBoolean(i++, c.chat());
         ps.setBoolean(i++, c.pkFightBack());
         ps.setInt(i++, c.spawnPerSecond());
         ps.setInt(i++, c.costumeChance());
         ps.setInt(i++, c.titleChance());
         ps.setInt(i++, c.petChance());
         ps.setInt(i++, c.maskChance());
         ps.setInt(i++, c.backChance());
         ps.setInt(i, ROW);
         if (ps.executeUpdate() == 0) {
            throw new SQLException("Chua co dong bot_profile id 1 - chay sql/migrations/2026-10-17_bot_profile.sql");
         }
      }
   }

   private static List<RewardItem> items(String json, String where) {
      return json == null || json.isBlank() ? List.of() : RewardItem.list(json, true, where);
   }

   /** {"loại": ["câu", ...]} -> mảng câu; JSON hỏng = rỗng (dùng câu mặc định). */
   static Map<String, String[]> chat(String json) {
      Map<String, String[]> out = new HashMap<>();
      if (json == null || json.isBlank()) {
         return out;
      }
      try {
         JsonObject o = JsonParser.parseString(json).getAsJsonObject();
         for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (e.getValue().isJsonArray()) {
               List<String> lines = new ArrayList<>();
               for (JsonElement l : (JsonArray) e.getValue()) {
                  String s = l.isJsonPrimitive() ? l.getAsString().trim() : "";
                  if (!s.isEmpty() && s.length() <= 80) {
                     lines.add(s);
                  }
               }
               if (!lines.isEmpty()) {
                  out.put(e.getKey(), lines.toArray(new String[0]));
               }
            }
         }
      } catch (RuntimeException e) {
         // JSON hỏng: giữ câu mặc định
      }
      return out;
   }
}
