package com.tlnt.server.daily;

import com.tlnt.server.db.Database;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Bảng {@code tlnt_data.daily_quest} ({@code name, data}) — xem {@link DailyConfig}. */
public final class DailyRepository {

   private final Database dataDb;

   public DailyRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Mọi dòng {@code name -> data}; null = đọc lỗi (giữ bản cũ). */
   Map<String, String> loadRows() {
      Map<String, String> rows = new HashMap<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT name, data FROM daily_quest");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            rows.put(rs.getString(1), rs.getString(2));
         }
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang daily_quest (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
      return rows;
   }
}
