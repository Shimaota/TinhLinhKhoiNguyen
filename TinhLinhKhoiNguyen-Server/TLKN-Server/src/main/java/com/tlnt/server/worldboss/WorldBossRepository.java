package com.tlnt.server.worldboss;

import com.tlnt.server.db.Database;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Đọc {@code tlnt_data.world_boss} ({@code id, name, active, maps, respawn_minutes, first_minutes, spec, loot} — định dạng
 * JSON ở {@link WorldBossDef} / {@link WorldBossLoot}; migration {@code sql/migrations/2026-10-08_world_boss.sql}).
 */
public final class WorldBossRepository {

   private final Database dataDb;

   public WorldBossRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Mọi boss đang bật (dòng sai định dạng: bỏ + cảnh báo). Lỗi SQL -> ném (người gọi giữ bản cũ). */
   List<WorldBossDef> loadActive() throws SQLException {
      List<WorldBossDef> out = new ArrayList<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id, name, maps, respawn_minutes, first_minutes, spec,"
                  + " loot FROM world_boss WHERE active <> 0 ORDER BY id");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            int id = rs.getInt(1);
            String where = "world_boss #" + id;
            try {
               out.add(WorldBossDef.parse(id, rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5),
                     rs.getString(6), rs.getString(7), where));
            } catch (RuntimeException e) {
               Log.warn("%s: cau hinh sai -> bo qua (%s)", where, e.getMessage());
            }
         }
      }
      return out;
   }
}
