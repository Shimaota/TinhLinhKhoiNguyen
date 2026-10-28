package com.tlnt.server.admin.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.util.Log;
import com.tlnt.server.worldboss.WorldBossService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tab "Dữ liệu game" của admin panel: xem / sửa / thêm / xoá dòng các bảng cấu hình trong {@code tlnt_data} (achievement,
 * fishing, guide_topic (quà Cẩm nang), item_appearance, level_exp, lucky_wheel, plant_template, server_list, shop, welfare, world_boss).
 * Mỗi bảng khai báo 1 {@link Table}: cột + kiểu + kiểu trình sửa (giao diện chọn widget theo {@code widget}). Tên bảng / cột
 * chỉ lấy từ khai báo (không từ request) -> SQL an toàn. Cột JSON: lúc đọc gắn thêm "name" / "icon" cho mọi object có "i"
 * (mẫu item) để hiện, lúc ghi bỏ đi và kiểm tra mẫu item tồn tại. Ghi xong nạp lại ngay mọi cấu hình ({@code reloadAll}).
 */
final class AdminData {

   /** 1 cột: {@code type} int / long / float / str / text / bool / json; {@code widget} = trình sửa JSON ở giao diện. */
   record Col(String name, String label, String type, String widget, boolean readOnly, String hint) {

      Map<String, Object> view() {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", name);
         m.put("label", label);
         m.put("type", type);
         if (widget != null) {
            m.put("widget", widget);
         }
         if (readOnly) {
            m.put("readOnly", true);
         }
         if (hint != null) {
            m.put("hint", hint);
         }
         return m;
      }
   }

   /** 1 bảng: khoá chính ({@code pk}), có cho thêm / xoá dòng không, cột tóm tắt hiện ở danh sách. */
   record Table(String name, String title, String description, List<String> pk, boolean insert, boolean delete,
         List<String> summary, List<Col> cols) {

      Col col(String n) {
         for (Col c : cols) {
            if (c.name.equals(n)) {
               return c;
            }
         }
         return null;
      }
   }

   private static Col c(String name, String label, String type) {
      return new Col(name, label, type, null, false, null);
   }

   private static Col c(String name, String label, String type, String hint) {
      return new Col(name, label, type, null, false, hint);
   }

   private static Col ro(String name, String label, String type, String hint) {
      return new Col(name, label, type, null, true, hint);
   }

   private static Col json(String name, String label, String widget, String hint) {
      return new Col(name, label, "json", widget, false, hint);
   }

   static final List<Table> TABLES = List.of(
         new Table("bot_profile", "Bot người chơi", "Cài đặt cơ bản của người chơi giả (1 dòng id 1) + món bộ sưu tập bot"
               + " được dùng (đặt dòng chỉ số; ghi 1 món nhiều lần = gặp nhiều hơn) + câu chat. Server đọc lại mỗi phút;"
               + " tab \"Bot người chơi\" sửa nhanh phần cài đặt.", List.of("id"), false, false,
               List.of("id", "enabled", "bot_count", "grind_percent"), List.of(
                     ro("id", "ID", "int", null),
                     c("enabled", "Bật bot", "bool"),
                     c("bot_count", "Số bot online", "int", "0-5000"),
                     c("grind_percent", "% đi up", "int"),
                     c("auto_percent", "% auto đánh (trong số đi up)", "int", "còn lại treo kiểu offline"),
                     c("fish_percent", "% câu cá", "int"),
                     c("afk_percent", "% AFK", "int", "còn lại: đứng chơi / đi dạo"),
                     c("party_percent", "% bot up đi theo nhóm", "int"),
                     c("boss_hunters", "Bot săn mỗi boss thế giới", "int", "0 = tắt"),
                     c("min_level", "Cấp thấp nhất", "int"),
                     c("max_level", "Cấp cao nhất", "int"),
                     c("zone_percent", "Tối đa % chỗ / khu", "int", "5-90, phần còn lại để dành người thật"),
                     c("session_min", "Online ít nhất (phút)", "int"),
                     c("session_max", "Online nhiều nhất (phút)", "int"),
                     c("chat", "Thỉnh thoảng chat khu", "bool"),
                     c("pk_fight_back", "Bị PK thì đánh trả / bỏ chạy", "bool"),
                     c("spawn_per_second", "Vào / ra mỗi giây", "int"),
                     c("costume_chance", "% có ngoại trang", "int", "cấp < 30 giảm dần, < 8 không có"),
                     c("title_chance", "% có danh hiệu", "int"),
                     c("pet_chance", "% có thú cưng", "int"),
                     c("mask_chance", "% có mặt nạ", "int"),
                     c("back_chance", "% có đeo lưng", "int"),
                     json("costumes", "Ngoại trang bot dùng", "rewards", "Chỉ số [min, max] = roll mỗi bot"),
                     json("titles", "Danh hiệu bot dùng", "rewards", "Chỉ món có dòng chỉ số"),
                     json("pets", "Thú cưng bot dùng", "rewards", null),
                     json("masks", "Mặt nạ bot dùng", "rewards", null),
                     json("backs", "Đeo lưng bot dùng", "rewards", null),
                     json("chat_lines", "Câu chat", "json",
                           "{\"idle\":[...],\"grind\":[...],\"party\":[...],\"fish\":[...],\"afk\":[...],\"boss\":[...],\"pk\":[...]}"))),
         new Table("achievement", "Thành tựu", "Thành tựu (op 62) — loại + mốc cần đạt + quà.", List.of("id"), true, true,
               List.of("id", "category", "title", "target"), List.of(
                     c("id", "ID", "int"),
                     c("category", "Loại", "str", "elite / leader / guardian / spirit / revive / market_sell / brew /"
                           + " activity_chest / upgrade_star / refine_stone / fish / beehive / pig_harvest / level"),
                     c("param", "Tham số", "int", "upgrade_star: số sao; level: cấp; còn lại 0"),
                     c("title", "Tên", "str"),
                     c("description", "Mô tả", "text"),
                     c("target", "Số cần đạt", "int"),
                     json("rewards", "Quà", "rewards", "Món không ghi khoá = KHOÁ."),
                     c("sort", "Thứ tự", "int"))),
         new Table("fishing", "Câu cá", "Cần câu, mồi và bảng cá (chỉ dùng dòng id nhỏ nhất).", List.of("id"), false,
               false, List.of("id", "rod_item", "bait_item"), List.of(
                     ro("id", "ID", "int", null),
                     c("rod_item", "Cần câu (mẫu item)", "int"),
                     c("bait_item", "Mồi (mẫu item)", "int", "Tốn 1 mỗi lần quăng cần"),
                     json("loot", "Cá / đồ câu được", "fishLoot",
                           "w = tỉ lệ % ở vùng VÀNG, wg = % ở vùng XANH (trống = bằng w). Tổng < 100: phần còn lại = cá"
                                 + " sổng."))),
         new Table("guide_topic", "Cẩm nang", "Mục cửa sổ Cẩm nang: tên / bố cục lấy từ data game client (sửa bằng"
               + " TLNT-Editor > Data client); ở đây chỉ sửa quà hiện trong mục (cột rewards, op 61).",
               List.of("category_idx", "idx"), false, false, List.of("category_idx", "id", "name"), List.of(
                     ro("category_idx", "Chương (thứ tự)", "int", "0 Cần biết, 1 Hoạt động"),
                     ro("idx", "Thứ tự mục", "int", null),
                     ro("id", "ID mục", "int", null),
                     ro("name", "Tên mục", "str", null),
                     json("rewards", "Quà hiển thị", "rewards", "Chỉ để xem trong cẩm nang (không phát quà); trống ="
                           + " không quà."))),
         new Table("item_appearance", "Ngoại hình item", "Item đang dùng đổi ngoại hình ra sao (chỉ server).",
               List.of("item_template_id"), true, true,
               List.of("item_template_id", "char_template_id", "skin_key", "title_key"), List.of(
                     c("item_template_id", "Mẫu item", "int"),
                     c("char_template_id", "Mẫu nhân vật ngoại trang / thú", "int", "char_template.id; để trống = không đổi"),
                     c("skin_key", "Skin", "str", "Áo / vũ khí / mặt nạ / đeo lưng: tên skin trong skeleton; trống ="
                           + " không"),
                     c("title_key", "Hiệu ứng danh hiệu", "str", "Skeleton hiệu ứng trên đầu; trống = không"))),
         new Table("level_exp", "EXP lên cấp", "EXP cần ở mỗi cấp để lên cấp kế (dòng cuối = cấp tối đa).",
               List.of("level"), true, true, List.of("level", "exp"), List.of(
                     c("level", "Cấp", "int"),
                     c("exp", "EXP cần", "long"))),
         new Table("lucky_wheel", "Vòng quay", "Vòng quay may mắn (op 71): dùng dòng đang bật có id nhỏ nhất, đúng 12"
               + " ô.", List.of("id"), true, true, List.of("id", "name", "active", "cost_item", "cost_qty"), List.of(
                     c("id", "ID", "int"),
                     c("name", "Tên", "str"),
                     c("active", "Bật", "bool"),
                     c("cost_item", "Vật phẩm quay (mẫu item)", "int"),
                     c("cost_qty", "Số lượng / lần", "int"),
                     json("slots", "12 ô", "wheelSlots", "Trọng số nhỏ = hiếm. % vĩnh viễn cho món có hạn dùng."))),
         new Table("plant_template", "Cây hái được", "Cây (dữ liệu hình ảnh lấy từ data game client — chỉ sửa cấu hình"
               + " hái).", List.of("id"), false, false, List.of("id", "name"), List.of(
                     ro("id", "ID", "int", null),
                     ro("name", "Tên", "str", null),
                     ro("offset_x", "Lệch X", "float", null),
                     ro("offset_y", "Lệch Y", "float", null),
                     ro("skeleton_name", "Skeleton", "str", null),
                     ro("idle_animation", "Animation", "str", null),
                     ro("skeleton_scale", "Tỉ lệ", "float", null),
                     json("harvest", "Cấu hình hái", "harvest", "Trống = cây không mọc."))),
         new Table("server_list", "Danh sách máy chủ", "Máy chủ hiện ở màn chọn server (op 43).", List.of("id"), true,
               true, List.of("id", "name", "host", "port", "enabled"), List.of(
                     c("id", "ID", "int"),
                     c("name", "Tên", "str"),
                     c("host", "Địa chỉ", "str"),
                     c("port", "Cổng", "int"),
                     c("sort_order", "Thứ tự", "int"),
                     c("enabled", "Bật", "bool"))),
         new Table("shop", "Cửa hàng NPC", "Shop NPC (op 55): 1 Selami, 2 Nimora, 3 Buraen, 4 Morgi...", List.of("id"),
               true, true, List.of("id", "name"), List.of(
                     c("id", "ID", "int"),
                     c("name", "Tên", "str"),
                     json("tabs", "Tab + hàng", "shopTabs", "Tiền: 0 vàng, 1 vàng khoá (thiếu bù vàng), 2 lam thạch, 3"
                           + " vật phẩm (mẫu item ở \"Item trả\")."))),
         new Table("welfare", "Phúc lợi", "Quà online / điểm danh / quà nạp (op 33). Đừng chèn mốc vào giữa (thứ tự = cờ"
               + " đã nhận).", List.of("tab"), false, false, List.of("tab", "name"), List.of(
                     ro("tab", "Tab", "int", "0 Online (phút), 1 Điểm danh (0 = thứ Hai .. 6 = CN), 2 Nạp (lam thạch)"),
                     c("name", "Tên", "str"),
                     json("milestones", "Mốc + quà", "milestones", "Món không ghi khoá = KHOÁ."))),
         new Table("weekly_top", "Top tuần", "Bảng Top tuần (op 73, tab \"Top tuần\" cửa sổ Cửa hàng). ID cố định 1 Giàu"
               + " có (nạp), 2 Cường hóa, 3 Chiến binh, 4 Ve chai — id quyết định cách tính điểm. Thưởng top 1-3 qua thư"
               + " thứ Hai 00:00.", List.of("id"), false, false, List.of("id", "sort", "name"), List.of(
                     ro("id", "ID", "int", "1 Giàu có, 2 Cường hóa, 3 Chiến binh, 4 Ve chai"),
                     c("sort", "Thứ tự tab", "int"),
                     c("name", "Tên tab", "str"),
                     json("top1", "Quà TOP 1", "rewards", "Món không ghi khoá = KHOÁ."),
                     json("top2", "Quà TOP 2", "rewards", "Món không ghi khoá = KHOÁ."),
                     json("top3", "Quà TOP 3", "rewards", "Món không ghi khoá = KHOÁ."))),
         new Table("world_boss", "Boss thế giới", "Boss thế giới (cả boss Lôi vực id 3-8).", List.of("id"), true, true,
               List.of("id", "name", "active", "maps", "respawn_minutes"), List.of(
                     c("id", "ID", "int"),
                     c("name", "Tên", "str"),
                     c("active", "Bật", "bool"),
                     json("maps", "Map", "intList", "Danh sách id map, cách nhau dấu phẩy"),
                     c("respawn_minutes", "Hồi sinh (phút)", "int"),
                     c("first_minutes", "Lần đầu sau (phút)", "int"),
                     json("spec", "Chỉ số (spec)", "json", "mob = boss quái, char = boss người chơi giả"),
                     json("loot", "Quà rơi", "loot", null))));

   private final AdminApi.Deps d;

   AdminData(AdminApi.Deps deps) {
      this.d = deps;
   }

   /** Danh sách bảng + cột (giao diện dựng form theo đây). */
   Object tables() {
      List<Map<String, Object>> out = new ArrayList<>();
      for (Table t : TABLES) {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("name", t.name);
         m.put("title", t.title);
         m.put("description", t.description);
         m.put("pk", t.pk);
         m.put("insert", t.insert);
         m.put("delete", t.delete);
         m.put("summary", t.summary);
         m.put("cols", t.cols.stream().map(Col::view).toList());
         out.add(m);
      }
      return out;
   }

   static Table table(String name) {
      for (Table t : TABLES) {
         if (t.name.equals(name)) {
            return t;
         }
      }
      throw new AdminApi.BadRequest("Không có bảng " + name + ".");
   }

   private static String columnList(Table t) {
      return String.join(", ", t.cols.stream().map(Col::name).toList());
   }

   /** Mọi dòng (bảng cấu hình nhỏ — vài trăm dòng). JSON trả dạng cây đã gắn tên / icon item. */
   Object rows(String name) throws SQLException {
      Table t = table(name);
      List<Map<String, Object>> out = new ArrayList<>();
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT " + columnList(t) + " FROM " + t.name + " ORDER BY "
                  + String.join(", ", t.pk));
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (Col col : t.cols) {
               row.put(col.name, read(rs, col));
            }
            out.add(row);
         }
      }
      return out;
   }

   private static Object read(ResultSet rs, Col col) throws SQLException {
      Object v = rs.getObject(col.name);
      if (v == null) {
         return null;
      }
      return switch (col.type) {
         case "int" -> rs.getInt(col.name);
         case "long" -> rs.getLong(col.name);
         case "float" -> rs.getDouble(col.name);
         case "bool" -> rs.getInt(col.name) != 0;
         case "json" -> {
            String s = rs.getString(col.name);
            try {
               yield named(JsonParser.parseString(s));
            } catch (RuntimeException e) {
               yield s; // JSON hỏng: trả chuỗi, giao diện cho sửa thô
            }
         }
         default -> rs.getString(col.name);
      };
   }

   /** Bản sao gắn "name" / "icon" vào mọi object có "i" (mẫu item). */
   static JsonElement named(JsonElement e) {
      if (e == null || e.isJsonNull()) {
         return JsonNull.INSTANCE;
      }
      if (e.isJsonArray()) {
         JsonArray out = new JsonArray();
         for (JsonElement x : e.getAsJsonArray()) {
            out.add(named(x));
         }
         return out;
      }
      if (e.isJsonObject()) {
         JsonObject out = new JsonObject();
         for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            out.add(en.getKey(), named(en.getValue()));
         }
         if (out.has("i") && out.get("i").isJsonPrimitive() && out.get("i").getAsJsonPrimitive().isNumber()) {
            var tpl = GameDataCache.itemTemplate(out.get("i").getAsInt());
            out.addProperty("name", tpl == null ? "?" : tpl.name());
            out.addProperty("icon", AdminItems.icon(tpl));
         }
         return out;
      }
      return e;
   }

   /** Bỏ "name" / "icon" (chỉ để hiển thị) khỏi object có "i" + kiểm tra mẫu item tồn tại, số lượng > 0. */
   static JsonElement clean(JsonElement e, String where) {
      if (e == null || e.isJsonNull()) {
         return JsonNull.INSTANCE;
      }
      if (e.isJsonArray()) {
         JsonArray out = new JsonArray();
         for (JsonElement x : e.getAsJsonArray()) {
            out.add(clean(x, where));
         }
         return out;
      }
      if (e.isJsonObject()) {
         JsonObject out = new JsonObject();
         boolean item = e.getAsJsonObject().has("i");
         for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            if (item && (en.getKey().equals("name") || en.getKey().equals("icon"))) {
               continue;
            }
            out.add(en.getKey(), clean(en.getValue(), where));
         }
         if (item) {
            int id;
            try {
               id = out.get("i").getAsInt();
            } catch (RuntimeException ex) {
               throw new AdminApi.BadRequest(where + ": \"i\" phải là số.");
            }
            if (GameDataCache.itemTemplate(id) == null) {
               throw new AdminApi.BadRequest(where + ": không có mẫu item " + id + ".");
            }
            if (out.has("q") && out.get("q").isJsonPrimitive() && out.get("q").getAsLong() <= 0) {
               throw new AdminApi.BadRequest(where + ": số lượng item " + id + " phải > 0.");
            }
         }
         return out;
      }
      return e;
   }

   /**
    * Lưu 1 dòng: {@code key} = giá trị khoá chính CŨ (thiếu = thêm dòng mới), {@code row} = giá trị mới các cột. Cột chỉ đọc
    * giữ nguyên. Kiểm tra theo bảng rồi ghi + nạp lại cấu hình.
    */
   Object save(JsonObject b) throws SQLException {
      Table t = table(AdminApi.str(b, "table"));
      JsonObject row = AdminApi.obj(b, "row");
      JsonObject key = b.has("key") && b.get("key").isJsonObject() ? b.getAsJsonObject("key") : null;
      if (key == null && !t.insert) {
         throw new AdminApi.BadRequest("Bảng " + t.title + " không cho thêm dòng.");
      }
      Map<String, Object> values = new LinkedHashMap<>();
      for (Col col : t.cols) {
         if (col.readOnly && key != null) {
            continue; // giữ nguyên
         }
         values.put(col.name, value(t, col, row.has(col.name) ? row.get(col.name) : JsonNull.INSTANCE));
      }
      validate(t, values, key);
      try (Connection c = d.dataDb().getConnection()) {
         if (key == null) {
            String cols = String.join(", ", values.keySet());
            String qs = String.join(", ", values.keySet().stream().map(x -> "?").toList());
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + t.name + " (" + cols + ") VALUES (" + qs
                  + ")")) {
               bind(ps, 1, t, values);
               ps.executeUpdate();
            } catch (SQLException e) {
               if (e.getErrorCode() == 1062) {
                  throw new AdminApi.BadRequest("Khoá chính đã tồn tại.");
               }
               throw e;
            }
         } else {
            String set = String.join(", ", values.keySet().stream().map(x -> x + " = ?").toList());
            String where = String.join(" AND ", t.pk.stream().map(x -> x + " = ?").toList());
            try (PreparedStatement ps = c.prepareStatement("UPDATE " + t.name + " SET " + set + " WHERE " + where)) {
               int n = bind(ps, 1, t, values);
               bindKey(ps, n, t, key);
               if (ps.executeUpdate() == 0) {
                  throw new AdminApi.BadRequest("Dòng không còn (đã bị xoá?).");
               }
            } catch (SQLException e) {
               if (e.getErrorCode() == 1062) {
                  throw new AdminApi.BadRequest("Khoá chính mới trùng dòng khác.");
               }
               throw e;
            }
         }
      }
      d.reloadAll().run();
      Log.info("Admin panel: %s dong %s %s", key == null ? "them" : "sua", t.name, keyText(t, values, key));
      return AdminApi.ok("Đã lưu " + t.title + " (đã nạp lại cấu hình).");
   }

   Object delete(JsonObject b) throws SQLException {
      Table t = table(AdminApi.str(b, "table"));
      if (!t.delete) {
         throw new AdminApi.BadRequest("Bảng " + t.title + " không cho xoá dòng.");
      }
      JsonObject key = AdminApi.obj(b, "key");
      String where = String.join(" AND ", t.pk.stream().map(x -> x + " = ?").toList());
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("DELETE FROM " + t.name + " WHERE " + where)) {
         bindKey(ps, 1, t, key);
         if (ps.executeUpdate() == 0) {
            throw new AdminApi.BadRequest("Dòng không còn.");
         }
      }
      d.reloadAll().run();
      Log.info("Admin panel: xoa dong %s %s", t.name, key);
      return AdminApi.ok("Đã xoá dòng (đã nạp lại cấu hình).");
   }

   private static String keyText(Table t, Map<String, Object> values, JsonObject key) {
      if (key != null) {
         return key.toString();
      }
      StringBuilder sb = new StringBuilder();
      for (String k : t.pk) {
         sb.append(k).append('=').append(values.get(k)).append(' ');
      }
      return sb.toString().trim();
   }

   /** Giá trị gửi lên -> giá trị ghi SQL theo kiểu cột (null = NULL). */
   private static Object value(Table t, Col col, JsonElement v) {
      boolean empty = v == null || v.isJsonNull() || v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()
            && v.getAsString().isBlank() && !col.type.equals("str") && !col.type.equals("text");
      String where = t.title + " / " + col.label;
      try {
         return switch (col.type) {
            case "int" -> empty ? null : v.getAsInt();
            case "long" -> empty ? null : v.getAsLong();
            case "float" -> empty ? null : v.getAsDouble();
            case "bool" -> !empty && (v.getAsJsonPrimitive().isBoolean() ? v.getAsBoolean() : v.getAsInt() != 0);
            case "json" -> {
               if (empty) {
                  yield null;
               }
               JsonElement parsed = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()
                     ? JsonParser.parseString(v.getAsString()) : v;
               yield clean(parsed, where).toString();
            }
            default -> v == null || v.isJsonNull() ? null : v.getAsString();
         };
      } catch (AdminApi.BadRequest e) {
         throw e;
      } catch (RuntimeException e) {
         throw new AdminApi.BadRequest(where + ": giá trị không hợp lệ (" + e.getMessage() + ").");
      }
   }

   private static int bind(PreparedStatement ps, int i, Table t, Map<String, Object> values) throws SQLException {
      for (Map.Entry<String, Object> e : values.entrySet()) {
         Col col = t.col(e.getKey());
         Object v = e.getValue();
         if (v == null) {
            ps.setNull(i++, switch (col.type) {
               case "int", "bool" -> Types.INTEGER;
               case "long" -> Types.BIGINT;
               case "float" -> Types.DOUBLE;
               default -> Types.VARCHAR;
            });
         } else if (v instanceof Boolean bo) {
            ps.setInt(i++, bo ? 1 : 0);
         } else {
            ps.setObject(i++, v);
         }
      }
      return i;
   }

   private static void bindKey(PreparedStatement ps, int i, Table t, JsonObject key) throws SQLException {
      for (String k : t.pk) {
         if (!key.has(k) || key.get(k).isJsonNull()) {
            throw new AdminApi.BadRequest("Thiếu khoá " + k + ".");
         }
         Col col = t.col(k);
         if (col.type.equals("int")) {
            ps.setInt(i++, key.get(k).getAsInt());
         } else {
            ps.setString(i++, key.get(k).getAsString());
         }
      }
   }

   // ------------------------------------------------------------------ kiểm tra theo bảng

   private static int intOf(Map<String, Object> v, String k) {
      Object o = v.get(k);
      return o == null ? 0 : ((Number) o).intValue();
   }

   private static String strOf(Map<String, Object> v, String k) {
      Object o = v.get(k);
      return o == null ? "" : o.toString();
   }

   private static void requireItem(Map<String, Object> v, String k, String label) {
      Object o = v.get(k);
      if (o == null || GameDataCache.itemTemplate(((Number) o).intValue()) == null) {
         throw new AdminApi.BadRequest(label + ": không có mẫu item " + o + ".");
      }
   }

   private static JsonElement jsonOf(Map<String, Object> v, String k) {
      Object o = v.get(k);
      return o == null ? JsonNull.INSTANCE : JsonParser.parseString(o.toString());
   }

   private static void requireArray(Map<String, Object> v, String k, String label) {
      if (!jsonOf(v, k).isJsonArray()) {
         throw new AdminApi.BadRequest(label + " phải là danh sách.");
      }
   }

   private static void requirePk(Table t, Map<String, Object> v) {
      for (String k : t.pk) {
         if (v.containsKey(k) && v.get(k) == null) {
            throw new AdminApi.BadRequest("Nhập " + t.col(k).label + ".");
         }
      }
   }

   private static void validate(Table t, Map<String, Object> v, JsonObject key) {
      requirePk(t, v);
      switch (t.name) {
         case "achievement" -> {
            if (strOf(v, "title").isBlank() || strOf(v, "category").isBlank()) {
               throw new AdminApi.BadRequest("Nhập tên + loại thành tựu.");
            }
            if (intOf(v, "target") < 1) {
               throw new AdminApi.BadRequest("Số cần đạt phải ≥ 1.");
            }
            if (com.tlnt.server.achievement.AchievementType.of(strOf(v, "category")) == null) {
               throw new AdminApi.BadRequest("Loại thành tựu '" + strOf(v, "category") + "' không có.");
            }
            requireArray(v, "rewards", "Quà");
            v.putIfAbsent("description", "");
            if (v.get("description") == null) {
               v.put("description", "");
            }
         }
         case "fishing" -> {
            requireItem(v, "rod_item", "Cần câu");
            requireItem(v, "bait_item", "Mồi");
            requireArray(v, "loot", "Bảng cá");
         }
         case "guide_topic" -> {
            if (v.get("rewards") != null && "[]".equals(v.get("rewards"))) {
               v.put("rewards", null); // không quà = NULL
            }
            if (v.get("rewards") != null && !jsonOf(v, "rewards").isJsonArray()) {
               throw new AdminApi.BadRequest("Quà phải là danh sách.");
            }
            if (v.get("rewards") != null && jsonOf(v, "rewards").getAsJsonArray().size() > 127) {
               throw new AdminApi.BadRequest("Tối đa 127 món / mục.");
            }
         }
         case "item_appearance" -> {
            requireItem(v, "item_template_id", "Mẫu item");
            int charTemplateId = intOf(v, "char_template_id");
            if (charTemplateId <= 0) {
               v.put("char_template_id", null);
            } else if (GameDataCache.charTemplate(charTemplateId) == null) {
               throw new AdminApi.BadRequest("Không có char_template.id=" + charTemplateId + ".");
            }
            for (String k : List.of("skin_key", "title_key")) {
               String value = strOf(v, k).trim();
               if (value.isEmpty()) {
                  v.put(k, null);
               } else if (value.length() > 255) {
                  throw new AdminApi.BadRequest(k + " tối đa 255 ký tự.");
               } else {
                  v.put(k, value);
               }
            }
         }
         case "level_exp" -> {
            if (intOf(v, "level") < 1) {
               throw new AdminApi.BadRequest("Cấp phải ≥ 1.");
            }
            Object exp = v.get("exp");
            if (exp == null || ((Number) exp).longValue() <= 0) {
               throw new AdminApi.BadRequest("EXP phải > 0.");
            }
         }
         case "lucky_wheel" -> {
            requireItem(v, "cost_item", "Vật phẩm quay");
            if (intOf(v, "cost_qty") < 1) {
               throw new AdminApi.BadRequest("Số lượng / lần phải ≥ 1.");
            }
            requireArray(v, "slots", "Ô vòng quay");
            JsonArray slots = jsonOf(v, "slots").getAsJsonArray();
            if (slots.size() != 12) {
               throw new AdminApi.BadRequest("Vòng quay cần đúng 12 ô (đang có " + slots.size() + ").");
            }
            for (JsonElement s : slots) {
               if (!s.isJsonObject() || !s.getAsJsonObject().has("item") || !s.getAsJsonObject().has("w")) {
                  throw new AdminApi.BadRequest("Mỗi ô cần trọng số \"w\" + \"item\".");
               }
            }
            if (strOf(v, "name").isBlank()) {
               throw new AdminApi.BadRequest("Nhập tên.");
            }
         }
         case "plant_template" -> {
            JsonElement h = jsonOf(v, "harvest");
            if (!h.isJsonNull() && !h.isJsonObject()) {
               throw new AdminApi.BadRequest("Cấu hình hái phải là object.");
            }
         }
         case "server_list" -> {
            if (strOf(v, "name").isBlank() || strOf(v, "host").isBlank()) {
               throw new AdminApi.BadRequest("Nhập tên + địa chỉ.");
            }
            int port = intOf(v, "port");
            if (port < 1 || port > 65535) {
               throw new AdminApi.BadRequest("Cổng 1-65535.");
            }
         }
         case "shop" -> {
            if (strOf(v, "name").isBlank()) {
               throw new AdminApi.BadRequest("Nhập tên shop.");
            }
            requireArray(v, "tabs", "Tab");
         }
         case "welfare" -> requireArray(v, "milestones", "Mốc");
         case "world_boss" -> {
            Object id = v.get("id") != null ? v.get("id") : key == null ? null : key.get("id").getAsInt();
            String error = WorldBossService.validate(id == null ? 0 : ((Number) id).intValue(), strOf(v, "name"),
                  strOf(v, "maps"), Math.max(1, intOf(v, "respawn_minutes")), Math.max(0, intOf(v, "first_minutes")),
                  strOf(v, "spec"), v.get("loot") == null ? "[]" : strOf(v, "loot"));
            if (error != null) {
               throw new AdminApi.BadRequest(error);
            }
            if (v.get("loot") == null) {
               v.put("loot", "[]");
            }
         }
         default -> {
         }
      }
   }
}
