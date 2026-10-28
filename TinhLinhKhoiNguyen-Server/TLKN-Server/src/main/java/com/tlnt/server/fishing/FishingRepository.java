package com.tlnt.server.fishing;

import com.google.gson.JsonObject;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng {@code tlnt_data.fishing} (1 dòng, id = 1; server nạp lại mỗi phút):
 * <pre>
 * rod_item  164   cần câu (item_template.id)
 * bait_item 152   mồi câu
 * loot      [{"i":251,"w":30,"wg":10}, {"i":177,"w":1,"wg":7}, ...]
 *           món = RewardItem (i q l s d) + w tỉ lệ % khi kéo ở vùng VÀNG + wg tỉ lệ % ở vùng XANH (bỏ = bằng w);
 *           "l" bỏ trống = KHOÁ
 * </pre>
 */
public final class FishingRepository {

   private final Database dataDb;

   public FishingRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** null nếu bảng chưa có / không có dòng / đọc lỗi (giữ bản cũ). */
   public FishingConfig load() {
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "SELECT rod_item, bait_item, loot FROM fishing ORDER BY id LIMIT 1");
            ResultSet rs = ps.executeQuery()) {
         if (!rs.next()) {
            Log.warn("Bang fishing trong -> dung cau hinh cau ca mac dinh");
            return null;
         }
         List<FishingConfig.Loot> loot = new ArrayList<>();
         for (JsonObject o : RewardItem.objects(rs.getString(3), "fishing.loot")) {
            RewardItem item = RewardItem.fromJson(o, true);
            int weight = o.has("w") ? o.get("w").getAsInt() : 0;
            int green = o.has("wg") ? o.get("wg").getAsInt() : weight;
            if (item != null && (weight > 0 || green > 0)) {
               loot.add(new FishingConfig.Loot(item, weight, green));
            }
         }
         return new FishingConfig(rs.getInt(1), rs.getInt(2), List.copyOf(loot));
      } catch (SQLException | RuntimeException e) {
         Log.warn("Khong doc duoc bang fishing (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
   }
}
