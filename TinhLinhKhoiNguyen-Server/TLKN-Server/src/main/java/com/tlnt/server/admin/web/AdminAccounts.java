package com.tlnt.server.admin.web;

import com.google.gson.JsonObject;
import com.tlnt.server.account.Account;
import com.tlnt.server.account.AccountService;
import com.tlnt.server.account.PasswordHasher;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.WorldPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Tab "Tài khoản" của admin panel: tìm / lọc, chi tiết (nhân vật + thư gần đây), tạo, đổi mật khẩu, khoá / mở khoá,
 * vai trò ({@code account.role}), MK cấp 2, xoá. Chạy trên luồng "admin" (được SQL).
 *
 * <p>Xoá: kick nếu đang online -> giữ tài khoản ({@code AccountService.adminHold}) -> từ chối nếu nhân vật còn trong bộ lạc
 * hoặc còn đồ đang kí gửi (RAM của 2 hệ thống đó là bản chuẩn) -> 1 transaction xoá thư, lịch sử kí gửi, tên cũ, nhân
 * vật, tài khoản.
 */
final class AdminAccounts {

   static final int PAGE_SIZE = 50;
   static final String[] ROLES = {"Người chơi", "GM", "Quản trị viên"};
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
         .withZone(ZoneId.systemDefault());

   private final AdminApi.Deps d;

   AdminAccounts(AdminApi.Deps deps) {
      this.d = deps;
   }

   // ================================================================== xem

   /**
    * Danh sách theo trang. {@code filter}: "" mọi tài khoản, "online", "banned", "staff" (GM / quản trị), "pw2" (có MK
    * cấp 2), "nochar" (chưa tạo nhân vật).
    */
   Object list(String text, String filter, int page) throws SQLException {
      List<Object> args = new ArrayList<>();
      StringBuilder where = new StringBuilder(" WHERE 1 = 1");
      String q = text == null ? "" : text.trim();
      if (!q.isEmpty()) {
         String like = AdminApi.like(q);
         where.append(" AND (a.username LIKE ? OR a.email LIKE ? OR p.name LIKE ? OR a.last_login_ip LIKE ?"
               + " OR a.register_ip LIKE ? OR a.id = ? OR p.id = ?)");
         for (int k = 0; k < 5; k++) {
            args.add(like);
         }
         int id = AdminApi.parseInt(q, -1);
         args.add(id);
         args.add(id);
      }
      switch (filter == null ? "" : filter) {
         case "banned" -> where.append(" AND a.banned <> 0");
         case "staff" -> where.append(" AND a.role > 0");
         case "pw2" -> where.append(" AND a.secondary_password_hash IS NOT NULL");
         case "nochar" -> where.append(" AND p.id IS NULL");
         case "online" -> {
            StringJoiner ids = new StringJoiner(",", " AND a.id IN (", ")");
            ids.add("-1");
            for (WorldPlayer wp : d.world().onlinePlayers()) {
               ids.add(String.valueOf(wp.data().getAccountId()));
            }
            where.append(ids);
         }
         default -> {
         }
      }
      String from = " FROM account a LEFT JOIN player p ON p.account_id = a.id" + where;
      Map<String, Object> out = new LinkedHashMap<>();
      try (Connection c = d.playerDb().getConnection()) {
         try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*)" + from)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
               rs.next();
               out.put("total", rs.getInt(1));
            }
         }
         int p = Math.max(0, page);
         try (PreparedStatement ps = c.prepareStatement("SELECT a.id, a.username, a.email, a.role, a.banned,"
               + " a.ban_reason, a.secondary_password_hash IS NOT NULL, a.created_at, a.last_login_at, a.last_login_ip,"
               + " p.id, p.name, p.level" + from + " ORDER BY a.id DESC LIMIT " + PAGE_SIZE + " OFFSET "
               + p * PAGE_SIZE)) {
            bind(ps, args);
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  Map<String, Object> row = new LinkedHashMap<>();
                  int id = rs.getInt(1);
                  row.put("id", id);
                  row.put("username", rs.getString(2));
                  row.put("email", rs.getString(3));
                  row.put("role", rs.getInt(4));
                  row.put("banned", rs.getBoolean(5));
                  row.put("banReason", rs.getString(6));
                  row.put("pw2", rs.getBoolean(7));
                  row.put("created", date(rs.getTimestamp(8)));
                  row.put("lastLogin", date(rs.getTimestamp(9)));
                  row.put("ip", rs.getString(10));
                  int pid = rs.getInt(11);
                  row.put("playerId", rs.wasNull() ? 0 : pid);
                  row.put("playerName", rs.getString(12));
                  row.put("level", rs.getInt(13));
                  row.put("online", d.accounts().isOnline(id));
                  rows.add(row);
               }
            }
            out.put("rows", rows);
            out.put("page", p);
            out.put("pageSize", PAGE_SIZE);
         }
      }
      return out;
   }

   /** Chi tiết 1 tài khoản + nhân vật + 30 thư gần nhất của nhân vật. */
   Object detail(int id) throws SQLException {
      Map<String, Object> out = new LinkedHashMap<>();
      try (Connection c = d.playerDb().getConnection()) {
         try (PreparedStatement ps = c.prepareStatement("SELECT a.id, a.username, a.email, a.role, a.banned,"
               + " a.ban_reason, a.secondary_password_hash IS NOT NULL, a.protection_enabled, a.register_ip,"
               + " a.created_at, a.last_login_at, a.last_login_ip, p.id, p.name, p.level, p.gender, p.gold,"
               + " p.locked_gold, p.diamond, p.play_time_seconds, p.last_logout_at"
               + " FROM account a LEFT JOIN player p ON p.account_id = a.id WHERE a.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
               if (!rs.next()) {
                  throw new AdminApi.BadRequest("Không có tài khoản #" + id + ".");
               }
               out.put("id", rs.getInt(1));
               out.put("username", rs.getString(2));
               out.put("email", rs.getString(3));
               out.put("role", rs.getInt(4));
               out.put("banned", rs.getBoolean(5));
               out.put("banReason", rs.getString(6));
               out.put("pw2", rs.getBoolean(7));
               out.put("protection", rs.getBoolean(8));
               out.put("registerIp", rs.getString(9));
               out.put("created", date(rs.getTimestamp(10)));
               out.put("lastLogin", date(rs.getTimestamp(11)));
               out.put("ip", rs.getString(12));
               int pid = rs.getInt(13);
               if (!rs.wasNull()) {
                  Map<String, Object> p = new LinkedHashMap<>();
                  p.put("id", pid);
                  p.put("name", rs.getString(14));
                  p.put("level", rs.getInt(15));
                  p.put("gender", com.tlnt.server.player.Gender.name(rs.getInt(16)));
                  p.put("gold", rs.getLong(17));
                  p.put("lockedGold", rs.getLong(18));
                  p.put("diamond", rs.getLong(19));
                  p.put("playTime", rs.getLong(20));
                  p.put("lastLogout", date(rs.getTimestamp(21)));
                  out.put("player", p);
               }
            }
         }
         out.put("online", d.accounts().isOnline(id));
         Object player = out.get("player");
         out.put("mails", player == null ? List.of() : mails(c, (Integer) ((Map<?, ?>) player).get("id")));
      }
      return out;
   }

   private static List<Map<String, Object>> mails(Connection c, int playerId) throws SQLException {
      List<Map<String, Object>> list = new ArrayList<>();
      try (PreparedStatement ps = c.prepareStatement("SELECT id, box, sender_name, receiver_name, subject, content,"
            + " items, is_read, claimed, created_at FROM mail WHERE owner_id = ? ORDER BY created_at DESC LIMIT 30")) {
         ps.setInt(1, playerId);
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Map<String, Object> row = new LinkedHashMap<>();
               row.put("id", rs.getInt(1));
               row.put("box", rs.getInt(2) == 0 ? "Đến" : "Đã gửi");
               row.put("from", rs.getString(3));
               row.put("to", rs.getString(4));
               row.put("subject", rs.getString(5));
               row.put("content", rs.getString(6));
               List<Map<String, Object>> items = new ArrayList<>();
               for (ItemData item : MailRepository.parseItems(rs.getString(7))) {
                  items.add(AdminItems.view(item, -1));
               }
               row.put("items", items);
               row.put("read", rs.getBoolean(8));
               row.put("claimed", rs.getBoolean(9));
               row.put("time", TIME.format(Instant.ofEpochMilli(rs.getLong(10))));
               list.add(row);
            }
         }
      }
      return list;
   }

   // ================================================================== thao tác

   Object create(JsonObject b) throws SQLException {
      String username = AdminApi.str(b, "username").trim();
      String password = AdminApi.str(b, "password");
      String email = AdminApi.str(b, "email").trim();
      if (email.isEmpty()) {
         email = username.toLowerCase(java.util.Locale.ROOT) + "@tlnt.local";
      }
      String error = AccountService.validateNewAccount(username, password, email);
      if (error != null) {
         throw new AdminApi.BadRequest(error);
      }
      int role = role(AdminApi.intField(b, "role"));
      int id;
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("INSERT INTO account (username, password_hash, email,"
                  + " register_ip, role) VALUES (?, ?, ?, 'admin', ?)", Statement.RETURN_GENERATED_KEYS)) {
         ps.setString(1, username);
         ps.setString(2, PasswordHasher.hash(password));
         ps.setString(3, email);
         ps.setInt(4, role);
         ps.executeUpdate();
         try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            id = keys.getInt(1);
         }
      } catch (SQLIntegrityConstraintViolationException duplicate) {
         throw new AdminApi.BadRequest("Tài khoản " + username + " đã tồn tại.");
      }
      Log.info("Admin panel: tao tai khoan #%d '%s' (vai tro %d)", id, username, role);
      Map<String, Object> out = AdminApi.ok("Đã tạo tài khoản " + username + " (#" + id + ").");
      out.put("id", id);
      return out;
   }

   Object password(int id, String password) throws SQLException {
      String error = AccountService.validatePassword(password);
      if (error != null) {
         throw new AdminApi.BadRequest(error);
      }
      update(id, "UPDATE account SET password_hash = ? WHERE id = ?", PasswordHasher.hash(password));
      Log.info("Admin panel: doi mat khau tai khoan #%d", id);
      return AdminApi.ok("Đã đổi mật khẩu tài khoản #" + id + " (có hiệu lực từ lần đăng nhập sau).");
   }

   Object email(int id, String email) throws SQLException {
      email = email == null ? "" : email.trim();
      if (email.isEmpty() || email.length() > 128 || !email.contains("@")) {
         throw new AdminApi.BadRequest("Email không hợp lệ.");
      }
      update(id, "UPDATE account SET email = ? WHERE id = ?", email);
      return AdminApi.ok("Đã đổi email.");
   }

   Object ban(int id, boolean banned, String reason) throws SQLException {
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("UPDATE account SET banned = ?, ban_reason = ? WHERE id = ?")) {
         ps.setBoolean(1, banned);
         ps.setString(2, banned && !reason.isBlank() ? reason : null);
         ps.setInt(3, id);
         if (ps.executeUpdate() == 0) {
            throw new AdminApi.BadRequest("Không có tài khoản #" + id + ".");
         }
      }
      if (banned) {
         d.accounts().adminKick(id, "Tài khoản đã bị khoá." + (reason.isBlank() ? "" : " Lý do: " + reason));
      }
      Log.info("Admin panel: %s tai khoan #%d", banned ? "khoa" : "mo khoa", id);
      return AdminApi.ok(banned ? "Đã khoá tài khoản #" + id + "." : "Đã mở khoá tài khoản #" + id + ".");
   }

   Object setRole(int id, int role) throws SQLException {
      int r = role(role);
      update(id, "UPDATE account SET role = ? WHERE id = ?", r);
      d.accounts().adminSetRole(id, r);
      Log.info("Admin panel: vai tro tai khoan #%d -> %d", id, r);
      return AdminApi.ok("Đã đặt vai trò " + ROLES[r] + " cho tài khoản #" + id + ".");
   }

   /** {@code action}: "set" đặt MK cấp 2 mới ({@code password}), "clear" xoá MK cấp 2 + tắt bảo vệ. */
   Object secondary(int id, String action, String password) throws SQLException {
      String hash = null;
      if ("set".equals(action)) {
         String error = AccountService.validateSecondary(password);
         if (error != null) {
            throw new AdminApi.BadRequest(error);
         }
         hash = PasswordHasher.hash(password);
      } else if (!"clear".equals(action)) {
         throw new AdminApi.BadRequest("Thao tác MK cấp 2 không hợp lệ.");
      }
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("UPDATE account SET secondary_password_hash = ?,"
                  + " protection_enabled = IF(? IS NULL, 0, protection_enabled) WHERE id = ?")) {
         ps.setString(1, hash);
         ps.setString(2, hash);
         ps.setInt(3, id);
         if (ps.executeUpdate() == 0) {
            throw new AdminApi.BadRequest("Không có tài khoản #" + id + ".");
         }
      }
      boolean protection = false;
      if (hash != null) {
         try (Connection c = d.playerDb().getConnection();
               PreparedStatement ps = c.prepareStatement("SELECT protection_enabled FROM account WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
               protection = rs.next() && rs.getBoolean(1);
            }
         }
      }
      d.accounts().adminSetSecondary(id, hash, protection);
      Log.info("Admin panel: %s MK cap 2 tai khoan #%d", hash == null ? "xoa" : "dat", id);
      return AdminApi.ok(hash == null ? "Đã xoá mật khẩu cấp 2 (tắt bảo vệ tài khoản)." : "Đã đặt mật khẩu cấp 2 mới.");
   }

   /** Xoá tài khoản + nhân vật (không hoàn tác). */
   Object delete(int id) throws SQLException {
      if (d.accounts().isOnline(id)) {
         d.accounts().adminKick(id, "Tài khoản đã bị quản trị viên xoá.");
         throw new AdminApi.BadRequest("Tài khoản đang online: đã kick, bấm xoá lại sau vài giây.");
      }
      if (!d.accounts().adminHold(id)) {
         throw new AdminApi.BadRequest("Tài khoản đang online / đang được xử lý, thử lại sau.");
      }
      try {
         int playerId = 0;
         String name = "";
         String username;
         try (Connection c = d.playerDb().getConnection();
               PreparedStatement ps = c.prepareStatement("SELECT a.username, p.id, p.name FROM account a"
                     + " LEFT JOIN player p ON p.account_id = a.id WHERE a.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
               if (!rs.next()) {
                  throw new AdminApi.BadRequest("Không có tài khoản #" + id + ".");
               }
               username = rs.getString(1);
               playerId = rs.getInt(2);
               name = rs.getString(3);
            }
         }
         if (playerId > 0) {
            if (d.tribes() != null && d.tribes().isMember(playerId)) {
               throw new AdminApi.BadRequest("Nhân vật " + name + " còn trong bộ lạc — cho rời / giải tán trước.");
            }
            if (count("SELECT COUNT(*) FROM market_listing WHERE seller_id = ? AND status = 0", playerId) > 0) {
               throw new AdminApi.BadRequest("Nhân vật " + name + " còn đồ đang kí gửi — chờ bán / hết hạn trước.");
            }
         }
         int pid = playerId;
         boolean ok = d.players().runOnWriter(conn -> { // sau mọi lượt lưu còn xếp hàng của nhân vật này
            if (pid > 0) {
               for (String sql : new String[] {"DELETE FROM mail WHERE owner_id = ?",
                     "DELETE FROM market_listing WHERE seller_id = ?",
                     "DELETE FROM player_name_history WHERE player_id = ?", "DELETE FROM player WHERE id = ?"}) {
                  try (PreparedStatement ps = conn.prepareStatement(sql)) {
                     ps.setInt(1, pid);
                     ps.executeUpdate();
                  }
               }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM account WHERE id = ?")) {
               ps.setInt(1, id);
               ps.executeUpdate();
            }
         }).join();
         if (!ok) {
            throw new AdminApi.BadRequest("Lỗi ghi CSDL, chưa xoá được (xem nhật ký).");
         }
         if (pid > 0 && d.rankings() != null) {
            d.rankings().remove(pid);
         }
         Log.info("Admin panel: XOA tai khoan #%d '%s' (nhan vat #%d '%s')", id, username, pid, Log.ascii(name));
         return AdminApi.ok("Đã xoá tài khoản " + username + (pid > 0 ? " và nhân vật " + name : "") + ".");
      } finally {
         d.accounts().adminRelease(id);
      }
   }

   // ================================================================== nội bộ

   private static int role(int role) {
      if (role < Account.ROLE_PLAYER || role > Account.ROLE_ADMIN) {
         throw new AdminApi.BadRequest("Vai trò 0-2.");
      }
      return role;
   }

   private void update(int id, String sql, Object value) throws SQLException {
      try (Connection c = d.playerDb().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
         ps.setObject(1, value);
         ps.setInt(2, id);
         if (ps.executeUpdate() == 0) {
            throw new AdminApi.BadRequest("Không có tài khoản #" + id + ".");
         }
      }
   }

   private int count(String sql, int arg) throws SQLException {
      try (Connection c = d.playerDb().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
         ps.setInt(1, arg);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
         }
      }
   }

   private static void bind(PreparedStatement ps, List<Object> args) throws SQLException {
      for (int k = 0; k < args.size(); k++) {
         ps.setObject(k + 1, args.get(k));
      }
   }

   static String date(Timestamp t) {
      return t == null ? "" : t.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
   }
}
