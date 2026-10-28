package com.tlnt.server.achievement;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Bảng {@code tlnt_data.achievement} (dữ liệu dump game gốc, migration 2026-10-08_achievement.sql). */
public final class AchievementRepository {

   private final Database dataDb;

   public AchievementRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /**
    * Mọi thành tựu theo {@code sort}. Loại lạ -> bỏ dòng; món quà có mẫu item server chưa có (game gốc mới hơn, vd 360
    * Huy hiệu Tinh Linh 1 ngày, 361 Thỏi vàng) -> bỏ món đó (gửi client mẫu lạ dễ lỗi), cảnh báo 1 lần / lần nạp.
    * @return null = đọc lỗi (giữ bản cũ)
    */
   public List<Achievement> loadAll() {
      List<Achievement> list = new ArrayList<>();
      Set<Integer> missing = new TreeSet<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id, category, param, title, description, target,"
                  + " rewards, sort FROM achievement ORDER BY sort, id");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            int id = rs.getInt("id");
            AchievementType type = AchievementType.of(rs.getString("category"));
            if (type == null) {
               Log.warn("Thanh tuu #%d: category '%s' la -> bo qua", id, rs.getString("category"));
               continue;
            }
            List<RewardItem> rewards = new ArrayList<>();
            for (RewardItem r : RewardItem.list(rs.getString("rewards"), true, "achievement #" + id)) {
               if (GameDataCache.itemTemplate(r.templateId()) != null) {
                  rewards.add(r);
               } else {
                  missing.add(r.templateId());
               }
            }
            list.add(new Achievement(id, type, rs.getInt("param"), rs.getString("title"), rs.getString("description"),
                  Math.max(1, rs.getInt("target")), List.copyOf(rewards), rs.getInt("sort")));
         }
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang achievement (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
      if (!missing.isEmpty()) {
         Log.warn("Thanh tuu: bo qua qua co mau item chua co trong item_template: %s", missing);
      }
      return list;
   }
}
