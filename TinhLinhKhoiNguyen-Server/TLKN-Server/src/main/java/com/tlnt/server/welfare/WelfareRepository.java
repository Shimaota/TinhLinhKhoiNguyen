package com.tlnt.server.welfare;

import com.google.gson.JsonObject;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc bảng quà {@code tlnt_data.welfare} (DB dữ liệu tĩnh, TLNT-Editor sửa): 1 dòng / tab, cột {@code milestones} JSON
 * <pre>
 * [{"target":10,"items":[{"i":150,"q":1,"l":1,"s":[[9,100]],"d":3}, ...]}, ...]
 * </pre>
 * target: tab 0 = phút online trong ngày, tab 1 = thứ (0 thứ Hai .. 6 Chủ nhật), tab 2 = lam thạch đã nạp. Món không
 * ghi "l" thì KHOÁ.
 */
public final class WelfareRepository {

   private final Database dataDb;

   public WelfareRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Nạp lại bảng quà vào {@link WelfareRewards}. Đọc lỗi (chưa chạy migration) -> cảnh báo, giữ bản cũ. */
   public void reload() {
      Map<WelfareTab, List<WelfareRewards.Milestone>> table = new EnumMap<>(WelfareTab.class);
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT tab, milestones FROM welfare ORDER BY tab");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            WelfareTab tab = WelfareTab.of(rs.getInt(1));
            if (tab == null) {
               continue;
            }
            List<WelfareRewards.Milestone> milestones = new ArrayList<>();
            String where = "welfare tab=" + tab.id;
            for (JsonObject m : RewardItem.objects(rs.getString(2), where)) {
               int target = m.has("target") ? m.get("target").getAsInt() : 0;
               String items = m.has("items") ? m.get("items").toString() : null;
               milestones.add(new WelfareRewards.Milestone(target, List.copyOf(RewardItem.list(items, true, where))));
            }
            table.put(tab, milestones);
         }
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang welfare (chay sql/migrations?): %s", e.getMessage());
         return;
      }
      WelfareRewards.load(table);
   }
}
