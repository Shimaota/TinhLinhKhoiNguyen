package com.tlnt.server.account;

import com.tlnt.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

/** Đọc/ghi bảng {@code account} (xem {@code sql/tlnt_player.sql}). */
public final class AccountRepository {

   private static final String SELECT = "SELECT id, username, password_hash, email, secondary_password_hash,"
         + " protection_enabled, banned, ban_reason, role FROM account";

   private final Database db;

   public AccountRepository(Database db) {
      this.db = db;
   }

   /** Tìm theo username (không phân biệt hoa thường nhờ collation _ci). */
   public Account findByUsername(String username) throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(SELECT + " WHERE username = ?")) {
         ps.setString(1, username);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? map(rs) : null;
         }
      }
   }

   /** @return id tài khoản mới, hoặc -1 nếu username đã tồn tại. */
   public int insert(String username, String passwordHash, String email, String ip) throws SQLException {
      String sql = "INSERT INTO account (username, password_hash, email, register_ip) VALUES (?, ?, ?, ?)";
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
         ps.setString(1, username);
         ps.setString(2, passwordHash);
         ps.setString(3, email);
         ps.setString(4, ip);
         ps.executeUpdate();
         try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getInt(1);
         }
      } catch (SQLIntegrityConstraintViolationException duplicate) {
         return -1;
      }
   }

   public void updateLastLogin(int accountId, String ip) throws SQLException {
      update("UPDATE account SET last_login_at = NOW(), last_login_ip = ? WHERE id = ?", ip, accountId);
   }

   public void updatePassword(int accountId, String hash) throws SQLException {
      update("UPDATE account SET password_hash = ? WHERE id = ?", hash, accountId);
   }

   public void updateSecondaryPassword(int accountId, String hash) throws SQLException {
      update("UPDATE account SET secondary_password_hash = ? WHERE id = ?", hash, accountId);
   }

   public void updateProtection(int accountId, boolean enabled) throws SQLException {
      update("UPDATE account SET protection_enabled = ? WHERE id = ?", enabled, accountId);
   }

   private void update(String sql, Object value, int accountId) throws SQLException {
      try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setObject(1, value);
         ps.setInt(2, accountId);
         ps.executeUpdate();
      }
   }

   private static Account map(ResultSet rs) throws SQLException {
      return new Account(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
            rs.getBoolean(6), rs.getBoolean(7), rs.getString(8), rs.getInt(9));
   }
}
