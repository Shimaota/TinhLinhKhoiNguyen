package com.tlnt.server.mail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.ItemCodec;
import com.tlnt.server.player.ItemData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bảng {@code mail} (DB nhân vật). Đọc (nạp hòm thư, tra tên người nhận) chạy trên luồng blocking; mọi lệnh GHI là
 * hàm tĩnh nhận {@link Connection} để chạy trong transaction của db-writer ({@code PlayerService.saveWith /
 * runOnWriter}) — cùng transaction với lượt lưu túi đồ.
 */
public final class MailRepository {

   /** Người chơi tìm theo tên (tên thật trong DB, đúng hoa thường). */
   public record PlayerRef(int id, String name) {
   }

   private static final String COLUMNS = "id, owner_id, box, sender_name, receiver_name, subject, content, items,"
         + " is_read, claimed, created_at";

   private final Database db;

   public MailRepository(Database db) {
      this.db = db;
   }

   /** Id lớn nhất đã dùng (server tự cấp id thư, tăng dần từ số này). */
   public int maxId() throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(id), 0) FROM mail");
            ResultSet rs = ps.executeQuery()) {
         rs.next();
         return rs.getInt(1);
      }
   }

   /** Tên nhân vật -> id (so khớp theo collation của cột, không phân biệt hoa thường). null = không có. */
   public PlayerRef findPlayer(String name) throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM player WHERE name = ?")) {
         ps.setString(1, name);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? new PlayerRef(rs.getInt(1), rs.getString(2)) : null;
         }
      }
   }

   /** Mọi thư (2 hộp) của 1 người. */
   public List<Mail> load(int ownerId) throws SQLException {
      List<Mail> mails = new ArrayList<>();
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM mail WHERE owner_id = ?")) {
         ps.setInt(1, ownerId);
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               mails.add(new Mail(rs.getInt("id"), rs.getInt("owner_id"), rs.getByte("box"),
                     rs.getString("sender_name"), rs.getString("receiver_name"), rs.getString("subject"),
                     rs.getString("content"), rs.getLong("created_at"), parseItems(rs.getString("items")),
                     rs.getBoolean("is_read"), rs.getBoolean("claimed")));
            }
         }
      }
      return mails;
   }

   // ------------------------------------------------------------------ ghi (trong transaction của db-writer)

   public static void insert(Connection conn, Mail mail) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("INSERT INTO mail (" + COLUMNS
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
         ps.setInt(1, mail.id);
         ps.setInt(2, mail.ownerId);
         ps.setByte(3, mail.box);
         ps.setString(4, mail.senderName);
         ps.setString(5, mail.receiverName);
         ps.setString(6, mail.subject);
         ps.setString(7, mail.content);
         ps.setString(8, mail.itemsJson);
         ps.setBoolean(9, mail.box == Mail.SENT);
         ps.setBoolean(10, mail.box == Mail.SENT);
         ps.setLong(11, mail.createdAt);
         ps.executeUpdate();
      }
   }

   /** Đã nhận đồ (kèm đã đọc). Chỉ chủ thư gọi. */
   public static void markClaimed(Connection conn, int ownerId, int mailId) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE mail SET claimed = 1, is_read = 1 WHERE id = ? AND owner_id = ?")) {
         ps.setInt(1, mailId);
         ps.setInt(2, ownerId);
         ps.executeUpdate();
      }
   }

   public static void markRead(Connection conn, int ownerId, int mailId) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("UPDATE mail SET is_read = 1 WHERE id = ? AND owner_id = ?")) {
         ps.setInt(1, mailId);
         ps.setInt(2, ownerId);
         ps.executeUpdate();
      }
   }

   public static void delete(Connection conn, Collection<Mail> mails) throws SQLException {
      if (mails.isEmpty()) {
         return;
      }
      try (PreparedStatement ps = conn.prepareStatement("DELETE FROM mail WHERE id = ? AND owner_id = ?")) {
         for (Mail mail : mails) {
            ps.setInt(1, mail.id);
            ps.setInt(2, mail.ownerId);
            ps.addBatch();
         }
         ps.executeBatch();
      }
   }

   /** Cột {@code items} -> item (JSON hỏng = không đính kèm). Admin panel đọc thư cũng dùng. */
   public static List<ItemData> parseItems(String json) {
      List<ItemData> items = new ArrayList<>();
      if (json == null || json.isBlank()) {
         return items;
      }
      try {
         JsonElement root = JsonParser.parseString(json);
         if (root.isJsonArray()) {
            for (JsonElement element : (JsonArray) root) {
               ItemData item = ItemCodec.fromJson(element);
               if (item != null) {
                  items.add(item.normalize());
               }
            }
         }
      } catch (RuntimeException ignored) {
         // JSON hỏng -> coi như không đính kèm
      }
      return items;
   }
}
