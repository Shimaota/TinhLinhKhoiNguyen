package com.tlnt.server.shop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bảng {@code tlnt_data.shop} (TLNT-Editor sửa, server tự nạp lại mỗi phút): id, tên, cột JSON {@code tabs}
 * <pre>
 * [{"name":"Tạp hóa","icon":"icon/281","items":[{"i":145,"q":1,"p":500,"cur":0,"l":0,"s":[[3,300]],"d":0}, ...]}]
 *  món = RewardItem (i q l s d) + p giá mỗi lần mua + cur tiền tệ (0 vàng, 1 vàng khoá, 2 lam thạch, 3 vật phẩm)
 *  + ci mẫu item trả khi cur = 3: {"i":150,"p":5,"cur":3,"ci":331} = mỗi lần mua tốn 5 Bùa chuyển hóa
 *  "l" bỏ trống: mua bằng vàng khoá thì món KHOÁ, còn lại không khoá
 *  lọc theo nhân vật (bỏ trống = ai cũng thấy):
 *   tab  "c":[2,3] chỉ class Rìu / Cung thấy tab, "g":[1] chỉ Rinn thấy tab -> cùng tên "Vũ khí" làm 5 tab cho 5
 *        class, mỗi class chỉ thấy tab của mình; tab không ghi c / g = mua chung
 *   món  "c":2 / "g":1 chỉ class / nhân vật đó thấy món (0 = tất cả); tab lọc xong rỗng thì ẩn
 * </pre>
 */
public final class ShopRepository {

   private final Database dataDb;

   public ShopRepository(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Mọi shop, key = id. null nếu đọc lỗi (giữ bản cũ). */
   public Map<Integer, Shop> loadAll() {
      Map<Integer, Shop> shops = new HashMap<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id, name, tabs FROM shop");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            int id = rs.getInt(1);
            String where = "shop #" + id;
            List<Shop.Tab> tabs = new ArrayList<>();
            for (JsonObject t : RewardItem.objects(rs.getString(3), where)) {
               List<Shop.Goods> goods = new ArrayList<>();
               String items = t.has("items") ? t.get("items").toString() : null;
               for (JsonObject o : RewardItem.objects(items, where)) {
                  int currency = o.has("cur") ? o.get("cur").getAsInt() : Shop.CUR_GOLD;
                  RewardItem item = RewardItem.fromJson(o, currency == Shop.CUR_LOCKED_GOLD);
                  int price = o.has("p") ? o.get("p").getAsInt() : 0;
                  int payItem = currency == Shop.CUR_ITEM && o.has("ci") ? o.get("ci").getAsInt() : 0;
                  if (currency == Shop.CUR_ITEM && payItem <= 0) {
                     Log.warn("%s: mon %s tra bang vat pham nhung thieu \"ci\" -> bo qua", where, o);
                     continue;
                  }
                  if (item != null && price > 0) {
                     goods.add(new Shop.Goods(item, price, currency, payItem, intOf(o, "c"), intOf(o, "g")));
                  }
               }
               tabs.add(new Shop.Tab(t.has("name") ? t.get("name").getAsString() : "Cửa hàng",
                     t.has("icon") ? t.get("icon").getAsString() : "icon/281", ids(t.get("c")), ids(t.get("g")),
                     List.copyOf(goods)));
            }
            shops.put(id, new Shop(id, rs.getString(2), List.copyOf(tabs)));
         }
         return shops;
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang shop (chay sql/migrations?): %s", e.getMessage());
         return null;
      }
   }

   private static int intOf(JsonObject o, String key) {
      return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : 0;
   }

   /** {@code [1,3]} hoặc 1 số -> tập id (bỏ 0); không có / rỗng = tập rỗng (= tất cả). */
   private static Set<Integer> ids(JsonElement e) {
      Set<Integer> set = new HashSet<>();
      if (e != null && e.isJsonArray()) {
         for (JsonElement v : e.getAsJsonArray()) {
            if (v.isJsonPrimitive() && v.getAsInt() > 0) {
               set.add(v.getAsInt());
            }
         }
      } else if (e != null && e.isJsonPrimitive() && e.getAsInt() > 0) {
         set.add(e.getAsInt());
      }
      return Set.copyOf(set);
   }
}
