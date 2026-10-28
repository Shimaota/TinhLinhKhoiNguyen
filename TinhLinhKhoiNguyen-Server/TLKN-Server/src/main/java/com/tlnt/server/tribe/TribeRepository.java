package com.tlnt.server.tribe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.ItemCodec;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.util.Log;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng {@code tribe} + {@code tribe_log} (DB nhân vật). Số đơn (cấp, điểm, quỹ, khẩu hiệu, biểu tượng, người giữ danh
 * hiệu) là CỘT riêng; mỗi danh sách (thành viên, đơn xin vào, kỹ năng, kho, thông báo, chỉ số biểu tượng) là 1 cột JSON
 * riêng -> sửa tay bằng SQL được (sửa khi server TẮT: RAM là bản chuẩn, đang chạy sẽ ghi đè).
 *
 * <p>Đọc hết lúc khởi động; mọi lệnh GHI là hàm tĩnh chạy trong transaction của db-writer. Dòng ghi = ẢNH CHỤP
 * {@link Row} lấy dưới khoá {@link TribeService} lúc xếp hàng -> thứ tự ghi = thứ tự thay đổi trong RAM.
 */
final class TribeRepository {

   private static final Gson GSON = new Gson();
   private static final Type MEMBERS = new TypeToken<List<Tribe.Member>>() { }.getType();
   private static final Type APPLICANTS = new TypeToken<List<Tribe.Applicant>>() { }.getType();
   private static final Type INTS = new TypeToken<List<Integer>>() { }.getType();
   private static final Type STRINGS = new TypeToken<List<String>>() { }.getType();

   private static final String COLUMNS = "id, name, name_key, slogan, level, exp, gold, diamond, emblem_id,"
         + " emblem_expire_at, emblem_lines, skills, top_points, top_diamond, top_gold, members, applicants, items,"
         + " notices, created_at";

   private final Database db;

   TribeRepository(Database db) {
      this.db = db;
   }

   /** 1 dòng đã chụp sẵn (JSON đã thành chuỗi), ghi được trên luồng khác. */
   record Row(int id, String name, String nameKey, String slogan, int level, long exp, long gold, long diamond,
         int emblemId, long emblemExpireAt, String emblemLines, String skills, int topPoints, int topDiamond,
         int topGold, String members, String applicants, String items, String notices, long createdAt) {
   }

   /** 1 dòng lịch sử ngân sách. */
   record LogEntry(int tribeId, long at, int playerId, String playerName, String kind, long gold, long diamond,
         String detail) {
   }

   int maxId() throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(id), 0) FROM tribe");
            ResultSet rs = ps.executeQuery()) {
         rs.next();
         return rs.getInt(1);
      }
   }

   List<Tribe> loadAll() throws SQLException {
      List<Tribe> tribes = new ArrayList<>();
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM tribe ORDER BY id");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            Row row = new Row(rs.getInt("id"), rs.getString("name"), rs.getString("name_key"),
                  rs.getString("slogan"), rs.getInt("level"), rs.getLong("exp"), rs.getLong("gold"),
                  rs.getLong("diamond"), rs.getInt("emblem_id"), rs.getLong("emblem_expire_at"),
                  rs.getString("emblem_lines"), rs.getString("skills"), rs.getInt("top_points"),
                  rs.getInt("top_diamond"), rs.getInt("top_gold"), rs.getString("members"),
                  rs.getString("applicants"), rs.getString("items"), rs.getString("notices"), rs.getLong("created_at"));
            try {
               tribes.add(fromRow(row));
            } catch (RuntimeException e) {
               Log.warn("Bo qua bo lac #%d: JSON hong (%s)", row.id(), e.getMessage());
            }
         }
      }
      return tribes;
   }

   /** Chụp {@code t} (gọi dưới khoá TribeService). */
   static Row snapshot(Tribe t) {
      JsonArray items = new JsonArray();
      for (ItemData item : t.items) {
         items.add(ItemCodec.toJson(item));
      }
      return new Row(t.id, t.name, TribeService.nameKey(t.name), t.slogan, t.level, t.exp, t.gold, t.diamond,
            t.emblemId, t.emblemExpireAt, GSON.toJson(t.emblemLines), GSON.toJson(t.skills), t.topPoints,
            t.topDiamond, t.topGold, GSON.toJson(t.members, MEMBERS), GSON.toJson(t.applicants, APPLICANTS),
            items.toString(), GSON.toJson(t.notices, STRINGS), t.createdAt);
   }

   /** Dựng lại bộ lạc từ 1 dòng; cột JSON trống / NULL = danh sách rỗng. */
   static Tribe fromRow(Row r) {
      Tribe t = new Tribe();
      t.id = r.id();
      t.name = r.name();
      t.slogan = r.slogan() == null ? "" : r.slogan();
      t.level = r.level();
      t.exp = r.exp();
      t.gold = r.gold();
      t.diamond = r.diamond();
      t.emblemId = r.emblemId() > 0 ? r.emblemId() : TribeRules.DEFAULT_EMBLEM;
      t.emblemExpireAt = r.emblemId() > 0 ? r.emblemExpireAt() : -1;
      int[][] lines = parse(r.emblemLines(), int[][].class);
      t.emblemLines = lines == null ? new int[0][] : lines;
      t.skills = list(parse(r.skills(), INTS));
      t.topPoints = r.topPoints();
      t.topDiamond = r.topDiamond();
      t.topGold = r.topGold();
      t.members = list(parse(r.members(), MEMBERS));
      t.members.removeIf(m -> m == null || m.id <= 0);
      t.applicants = list(parse(r.applicants(), APPLICANTS));
      t.notices = list(parse(r.notices(), STRINGS));
      t.items = new ArrayList<>();
      if (r.items() != null && !r.items().isBlank()) {
         JsonElement items = JsonParser.parseString(r.items());
         if (items.isJsonArray()) {
            for (JsonElement e : items.getAsJsonArray()) {
               ItemData item = ItemCodec.fromJson(e);
               if (item != null) {
                  t.items.add(item.normalize());
               }
            }
         }
      }
      t.createdAt = r.createdAt();
      return t;
   }

   static void save(Connection conn, Row r) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tribe (" + COLUMNS + ", updated_at) VALUES"
            + " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE"
            + " name = VALUES(name), name_key = VALUES(name_key), slogan = VALUES(slogan), level = VALUES(level),"
            + " exp = VALUES(exp), gold = VALUES(gold), diamond = VALUES(diamond), emblem_id = VALUES(emblem_id),"
            + " emblem_expire_at = VALUES(emblem_expire_at), emblem_lines = VALUES(emblem_lines),"
            + " skills = VALUES(skills), top_points = VALUES(top_points), top_diamond = VALUES(top_diamond),"
            + " top_gold = VALUES(top_gold), members = VALUES(members), applicants = VALUES(applicants),"
            + " items = VALUES(items), notices = VALUES(notices), updated_at = VALUES(updated_at)")) {
         int i = 1;
         ps.setInt(i++, r.id());
         ps.setString(i++, r.name());
         ps.setString(i++, r.nameKey());
         ps.setString(i++, r.slogan());
         ps.setInt(i++, r.level());
         ps.setLong(i++, r.exp());
         ps.setLong(i++, r.gold());
         ps.setLong(i++, r.diamond());
         ps.setInt(i++, r.emblemId());
         ps.setLong(i++, r.emblemExpireAt());
         ps.setString(i++, r.emblemLines());
         ps.setString(i++, r.skills());
         ps.setInt(i++, r.topPoints());
         ps.setInt(i++, r.topDiamond());
         ps.setInt(i++, r.topGold());
         ps.setString(i++, r.members());
         ps.setString(i++, r.applicants());
         ps.setString(i++, r.items());
         ps.setString(i++, r.notices());
         ps.setLong(i++, r.createdAt());
         ps.setLong(i, System.currentTimeMillis());
         ps.executeUpdate();
      }
   }

   static void delete(Connection conn, int tribeId) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tribe WHERE id = ?")) {
         ps.setInt(1, tribeId);
         ps.executeUpdate();
      }
   }

   static void log(Connection conn, LogEntry e) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tribe_log (tribe_id, at, player_id, player_name,"
            + " kind, gold, diamond, detail) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
         ps.setInt(1, e.tribeId());
         ps.setLong(2, e.at());
         ps.setInt(3, e.playerId());
         ps.setString(4, e.playerName());
         ps.setString(5, e.kind());
         ps.setLong(6, e.gold());
         ps.setLong(7, e.diamond());
         String detail = e.detail() == null ? "" : e.detail();
         ps.setString(8, detail.length() > 255 ? detail.substring(0, 255) : detail);
         ps.executeUpdate();
      }
   }

   /** Người chơi đang offline bị đuổi: ghi thẳng mốc rời bộ lạc vào dòng nhân vật. */
   static void setLeftAt(Connection conn, int playerId, long at) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("UPDATE player SET tribe_left_at = ? WHERE id = ?")) {
         ps.setLong(1, at);
         ps.setInt(2, playerId);
         ps.executeUpdate();
      }
   }

   private static <T> T parse(String json, Type type) {
      return json == null || json.isBlank() ? null : GSON.fromJson(json, type);
   }

   private static <T> List<T> list(List<T> value) {
      return value == null ? new ArrayList<>() : new ArrayList<>(value);
   }
}
