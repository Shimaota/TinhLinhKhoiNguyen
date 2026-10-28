package com.tlnt.server.event;

import com.tlnt.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Cờ bật / tắt sự kiện trong {@code tlnt_server.server_state} (v = "1" / "0"; chưa có dòng = tắt). */
public final class EventRepository {

   private final Database playerDb;

   public EventRepository(Database playerDb) {
      this.playerDb = playerDb;
   }

   boolean enabled(String key) throws SQLException {
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT v FROM server_state WHERE k = ?")) {
         ps.setString(1, key);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() && "1".equals(rs.getString(1).trim());
         }
      }
   }

   /** Giá trị chuỗi của khoá, null nếu chưa có. */
   String get(String key) throws SQLException {
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT v FROM server_state WHERE k = ?")) {
         ps.setString(1, key);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
         }
      }
   }

   void put(String key, String value) throws SQLException {
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "INSERT INTO server_state (k, v) VALUES (?, ?) ON DUPLICATE KEY UPDATE v = VALUES(v)")) {
         ps.setString(1, key);
         ps.setString(2, value);
         ps.executeUpdate();
      }
   }

   void save(String key, boolean on) throws SQLException {
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "INSERT INTO server_state (k, v) VALUES (?, ?) ON DUPLICATE KEY UPDATE v = VALUES(v)")) {
         ps.setString(1, key);
         ps.setString(2, on ? "1" : "0");
         ps.executeUpdate();
      }
   }
}
