package com.tlnt.server.combo;

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
 * Đọc {@code tlnt_data.combo} (DB dữ liệu tĩnh): {@code id, name, active, packs}. Cột {@code packs} JSON:
 * <pre>
 * [{"name":"Gói combo Thợ săn","pi":2,"p":750,"limit":1,"c":0,"g":0,
 *   "items":[{"i":241,"l":1,"s":[[41,100],[9,100],[19,350]]},{"i":176,"l":1},{"i":292,"q":10}]}, ...]
 * </pre>
 * {@code pi} mẫu item trả tiền (1 vàng — trừ vàng khoá trước, 2 lam thạch (mặc định), khác = vật phẩm trong túi),
 * {@code p} giá, {@code limit} số lần mỗi nhân vật mua (bỏ = 1, 0 = không giới hạn), {@code c} / {@code g} chỉ class /
 * nhân vật đó thấy (bỏ = tất cả), {@code items} = RewardItem (món không ghi "l" thì KHÔNG khoá).
 * Số lần đã mua lưu theo VỊ TRÍ gói -> đừng chèn gói vào giữa danh mục đang bán (thêm cuối / tạo danh mục id mới).
 */
public final class ComboRepository {

   private final Database dataDb;

   public ComboRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Các danh mục đang bật, id tăng dần. null nếu đọc lỗi (giữ bản cũ). */
   List<Combo> loadActive() {
      List<Combo> list = new ArrayList<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id, name, packs FROM combo WHERE active <> 0 ORDER BY id");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            int id = rs.getInt(1);
            String where = "combo #" + id;
            List<Combo.Pack> packs = new ArrayList<>();
            for (JsonObject o : RewardItem.objects(rs.getString(3), where)) {
               List<RewardItem> items = RewardItem.list(o.has("items") ? o.get("items").toString() : null, false, where);
               for (RewardItem item : items) {
                  if (GameDataCache.itemTemplate(item.templateId()) == null) {
                     Log.warn("%s: khong co item_template id=%d", where, item.templateId());
                  }
               }
               packs.add(new Combo.Pack(o.has("name") ? o.get("name").getAsString() : "Gói combo", List.copyOf(items),
                     intOf(o, "pi", Combo.PAY_DIAMOND), intOf(o, "p", 0), intOf(o, "limit", 1), intOf(o, "c", 0),
                     intOf(o, "g", 0)));
            }
            list.add(new Combo(id, rs.getString(2), List.copyOf(packs)));
         }
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang combo (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
      return list;
   }

   private static int intOf(JsonObject o, String key, int fallback) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : fallback;
   }
}
