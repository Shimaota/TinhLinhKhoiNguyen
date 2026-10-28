package com.tlnt.server.wheel;

import com.google.gson.JsonObject;
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

/**
 * Đọc {@code tlnt_data.lucky_wheel}: {@code id, name, active, cost_item, cost_qty, slots}. Cột {@code slots} JSON — mỗi
 * phần tử 1 ô, đúng thứ tự trên vòng quay (client 12 ô):
 * <pre>
 * [{"w":2500,"item":{"i":1,"q":50000,"l":1}},
 *  {"w":60,"perm":3,"item":{"i":192,"s":[[8,[1,5]],[14,[1,5]],[44,[1,5]],[57,[1,3]],[55]]}}, ...]
 * </pre>
 * {@code w} trọng số (tỉ lệ = w / tổng mọi ô — nhỏ = hiếm, tạo khan hiếm), {@code perm} % ra vĩnh viễn cho món có dòng
 * 57 "HSD {0} ngày" (bỏ = {@link LuckyWheelService#DEFAULT_PERM_PERCENT} nếu món có dòng 55 "Có tỉ lệ vĩnh viễn",
 * không thì 0), {@code item} = RewardItem (món không ghi "l" thì KHÔNG khoá).
 */
public final class LuckyWheelRepository {

   private final Database dataDb;

   public LuckyWheelRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Vòng quay đang bật có id nhỏ nhất, null = không có. Lỗi đọc -> ném (người gọi giữ bản cũ). */
   LuckyWheel loadActive() throws SQLException {
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "SELECT id, name, cost_item, cost_qty, slots FROM lucky_wheel WHERE active <> 0 ORDER BY id LIMIT 1");
            ResultSet rs = ps.executeQuery()) {
         if (!rs.next()) {
            return null;
         }
         int id = rs.getInt(1);
         String where = "lucky_wheel #" + id;
         List<LuckyWheel.Slot> slots = new ArrayList<>();
         int total = 0;
         for (JsonObject o : RewardItem.objects(rs.getString(5), where)) {
            RewardItem item = o.has("item") && o.get("item").isJsonObject()
                  ? RewardItem.fromJson(o.getAsJsonObject("item"), false) : null;
            int weight = o.has("w") ? Math.max(0, o.get("w").getAsInt()) : 0;
            if (item == null) {
               Log.warn("%s: o %d thieu \"item\" -> bo qua", where, slots.size());
               continue;
            }
            if (GameDataCache.itemTemplate(item.templateId()) == null) {
               Log.warn("%s: khong co item_template id=%d", where, item.templateId());
            }
            int perm = o.has("perm") ? o.get("perm").getAsInt()
                  : LuckyWheelService.hasLine(item, LuckyWheelService.STAT_PERMANENT_CHANCE)
                        ? LuckyWheelService.DEFAULT_PERM_PERCENT : 0;
            slots.add(new LuckyWheel.Slot(item, weight, Math.max(0, Math.min(100, perm))));
            total += weight;
         }
         if (slots.isEmpty() || total <= 0) {
            Log.warn("%s: khong co o nao / tong trong so = 0 -> tat vong quay", where);
            return null;
         }
         return new LuckyWheel(id, rs.getString(2), rs.getInt(3), Math.max(1, rs.getInt(4)), List.copyOf(slots), total);
      }
   }
}
