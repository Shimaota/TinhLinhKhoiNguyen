package com.tlnt.server.admin.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.gather.GatherConfig;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.plant.PlantConfig;
import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Can thiệp thế giới từ admin panel: mini boss Tiểu Tinh linh Cáo (trạng thái, vàng đã trộm của ai, hạ, trả vàng), dời
 * boss thế giới / cáo sang map / khu / chỗ khác, xoá / đặt thêm tài nguyên (cây hái được, tổ ong, quả dừa). Mọi việc
 * chạm khu đều xếp vào luồng tick của khu ({@code World.admin*}), luồng admin chờ tối đa {@link #WAIT_MS}.
 */
final class AdminWorld {

   static final long WAIT_MS = 2_000;
   /** Tối đa số tài nguyên đặt thêm / khu / lần. */
   static final int MAX_ADD = 20;

   private final AdminApi.Deps d;
   private final AdminPlayers players;

   AdminWorld(AdminApi.Deps deps, AdminPlayers players) {
      this.d = deps;
      this.players = players;
   }

   private static <T> T await(java.util.concurrent.CompletableFuture<T> f) {
      try {
         return f.get(WAIT_MS, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
         throw new AdminApi.BadRequest("Khu không trả lời kịp, thử lại.");
      }
   }

   // ================================================================== mini boss Cáo

   /** Danh sách 5 con cáo + với con đang sống: HP, vàng đã trộm, ai bị trộm bao nhiêu, đang nhắm ai. */
   Object foxes() {
      List<Map<String, Object>> list = d.foxes() == null ? new ArrayList<>() : d.foxes().adminList();
      for (Map<String, Object> row : list) {
         if (Boolean.TRUE.equals(row.get("alive"))) {
            try {
               Map<String, Object> st = d.world().adminFoxState((Integer) row.get("mapId"), (Integer) row.get("zoneId"),
                     (Integer) row.get("key"), false).get(500, TimeUnit.MILLISECONDS);
               if (st != null) {
                  named(st);
                  row.putAll(st);
               }
            } catch (Exception e) {
               row.put("hp", -1);
            }
         }
      }
      return list;
   }

   /** Tên người bị trộm đang offline lấy từ SQL. */
   @SuppressWarnings("unchecked")
   private void named(Map<String, Object> st) {
      for (Object[] v : (List<Object[]>) st.get("victims")) {
         if (((String) v[1]).isEmpty()) {
            var online = d.world().online((Integer) v[0]);
            v[1] = online != null ? online.data().name : nameOf((Integer) v[0]);
         }
      }
   }

   private String nameOf(int playerId) {
      try (var c = d.playerDb().getConnection();
            var ps = c.prepareStatement("SELECT name FROM player WHERE id = ?")) {
         ps.setInt(1, playerId);
         try (var rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "#" + playerId;
         }
      } catch (SQLException e) {
         return "#" + playerId;
      }
   }

   private Map<String, Object> fox(int slot) {
      if (d.foxes() == null) {
         throw new AdminApi.BadRequest("Chưa bật mini boss Cáo.");
      }
      for (Map<String, Object> row : d.foxes().adminList()) {
         if ((Integer) row.get("slot") == slot) {
            return row;
         }
      }
      throw new AdminApi.BadRequest("Không có cáo #" + slot + ".");
   }

   Object foxKill(int slot) {
      Map<String, Object> row = fox(slot);
      if (!Boolean.TRUE.equals(row.get("alive"))) {
         throw new AdminApi.BadRequest("Con cáo này không đang sống.");
      }
      d.foxes().adminKill(slot);
      Log.info("Admin panel: ha cao #%d", slot);
      return AdminApi.ok("Đã hạ " + row.get("name") + " (vàng đã trộm rơi xuống đất).");
   }

   Object foxSpawn() {
      if (d.foxes() == null) {
         throw new AdminApi.BadRequest("Chưa bật mini boss Cáo.");
      }
      d.foxes().adminSpawnAll();
      return AdminApi.ok("Đã cho mọc các con cáo chưa có.");
   }

   /** Trả vàng khoá cho từng người bị con cáo {@code slot} trộm (thư hệ thống) rồi xoá sổ trộm của nó. */
   @SuppressWarnings("unchecked")
   Object foxRefund(int slot) throws SQLException {
      Map<String, Object> row = fox(slot);
      if (!Boolean.TRUE.equals(row.get("alive"))) {
         throw new AdminApi.BadRequest("Con cáo này không đang sống.");
      }
      Map<String, Object> st = await(d.world().adminFoxState((Integer) row.get("mapId"), (Integer) row.get("zoneId"),
            (Integer) row.get("key"), true));
      if (st == null) {
         throw new AdminApi.BadRequest("Con cáo vừa đổi khu / bị hạ, thử lại.");
      }
      int mails = 0;
      long gold = 0;
      for (Object[] v : (List<Object[]>) st.get("victims")) {
         long amount = (Long) v[2];
         if (amount <= 0) {
            continue;
         }
         JsonArray items = new JsonArray();
         JsonObject g = new JsonObject();
         g.addProperty("i", 1);
         g.addProperty("q", amount);
         g.addProperty("l", 1);
         items.add(g);
         players.gift((Integer) v[0], items, "Trả lại vàng bị Cáo trộm",
               "Quản trị viên trả lại số vàng khoá Tiểu Tinh linh Cáo đã lấy trộm của bạn.");
         mails++;
         gold += amount;
      }
      Log.info("Admin panel: tra %d vang khoa cao #%d trom cho %d nguoi", gold, slot, mails);
      return AdminApi.ok("Đã trả " + gold + " vàng khoá cho " + mails + " người qua thư; sổ trộm của con cáo đã xoá.");
   }

   // ================================================================== dời boss

   /**
    * Dời boss: {@code id} &lt; 1000 = boss thế giới (world_boss.id), ≥ 1000 = cáo (1000 + slot). Đích: map, khu (trống = giữ
    * khu hiện tại nếu cùng map, không thì khu 1), x / y tuỳ chọn (trống = chỗ ngẫu nhiên xa cửa).
    */
   Object moveBoss(JsonObject b) {
      int id = AdminApi.intField(b, "id");
      Map<String, Object> row = null;
      if (id >= 1000 && d.foxes() != null) {
         for (Map<String, Object> r : d.foxes().adminList()) {
            if ((Integer) r.get("key") == id) {
               row = r;
            }
         }
      } else {
         for (Map<String, Object> r : d.bosses().adminList()) {
            if ((Integer) r.get("id") == id) {
               row = r;
            }
         }
      }
      if (row == null || !Boolean.TRUE.equals(row.get("alive"))) {
         throw new AdminApi.BadRequest("Boss không đang sống.");
      }
      int fromMap = (Integer) row.get("mapId");
      int fromZone = (Integer) row.get("zoneId");
      int toMap = AdminApi.intField(b, "map");
      if (d.world().mapTemplate(toMap) == null) {
         throw new AdminApi.BadRequest("Không có map " + toMap + ".");
      }
      int toZone = b.has("zone") && !b.get("zone").isJsonNull() ? AdminApi.intField(b, "zone")
            : toMap == fromMap ? fromZone : 1; // trống = giữ khu (cùng map) / khu 1
      Float x = AdminApi.optFloat(b, "x");
      Float y = AdminApi.optFloat(b, "y");
      String error = await(d.world().adminMoveWorldBoss(id, fromMap, fromZone, toMap, toZone, x, y));
      if (error != null) {
         throw new AdminApi.BadRequest(error);
      }
      Log.info("Admin panel: doi boss #%d map %d khu %d -> map %d khu %d", id, fromMap, fromZone, toMap, toZone);
      return AdminApi.ok("Đã dời " + row.get("name") + " tới " + d.mapName(toMap) + " khu " + toZone
            + (x == null ? " (chỗ ngẫu nhiên)" : " (" + Math.round(x) + ", " + (y == null ? "mặt đất" : Math.round(y)) + ")")
            + ".");
   }

   // ================================================================== tài nguyên

   /** Loại tài nguyên đặt được: cây (PlantConfig) + quái săn bắt (GatherConfig) kèm đồ nhặt được + map cấu hình. */
   Object resourceTypes() {
      List<Map<String, Object>> out = new ArrayList<>();
      for (PlantConfig.Entry e : PlantConfig.current().byTemplate().values()) {
         var t = GameDataCache.plant(e.templateId());
         out.add(type("plant", e.templateId(), t == null ? "Cây #" + e.templateId() : t.name(), e.items(),
               new ArrayList<>(e.maps())));
      }
      for (GatherConfig.Entry e : GatherConfig.current().byMob().values()) {
         var m = GameDataCache.mob(e.mobId());
         out.add(type(e.mobId() == GatherConfig.HIVE_MOB ? "hive" : "coconut", e.mobId(),
               m == null ? "Quái #" + e.mobId() : m.name(), e.items(), new ArrayList<>(e.spots().keySet())));
      }
      return out;
   }

   private static Map<String, Object> type(String kind, int id, String name, List<RewardItem> items, List<Integer> maps) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("kind", kind);
      row.put("templateId", id);
      row.put("name", name);
      List<Map<String, Object>> list = new ArrayList<>();
      for (RewardItem r : items) {
         var t = GameDataCache.itemTemplate(r.templateId());
         Map<String, Object> it = new LinkedHashMap<>();
         it.put("id", r.templateId());
         it.put("name", t == null ? "#" + r.templateId() : t.name());
         it.put("icon", AdminItems.icon(t));
         list.add(it);
      }
      row.put("items", list);
      maps.sort(Integer::compare);
      row.put("maps", maps);
      return row;
   }

   Object resourceRemove(JsonObject b) {
      String kind = AdminApi.str(b, "kind");
      String error = await(d.world().adminRemoveResource(kind, AdminApi.intField(b, "map"), AdminApi.intField(b, "zone"),
            AdminApi.intField(b, "entityId")));
      if (error != null) {
         throw new AdminApi.BadRequest(error);
      }
      Log.info("Admin panel: xoa tai nguyen %s #%d map %d khu %d", kind, AdminApi.intField(b, "entityId"),
            AdminApi.intField(b, "map"), AdminApi.intField(b, "zone"));
      return AdminApi.ok("Đã xoá. Cây đặt sẵn của map mọc lại đúng chỗ sau thời gian hồi; cây / tổ ong ngẫu nhiên mọc lại"
            + " chỗ khác; cái admin đặt thì mất hẳn.");
   }

   Object resourceAdd(JsonObject b) {
      String kind = AdminApi.str(b, "kind");
      int tpl = AdminApi.intField(b, "templateId");
      int map = AdminApi.intField(b, "map");
      int zone = b.has("zone") && !b.get("zone").isJsonNull() ? AdminApi.intField(b, "zone") : -1; // -1 = mọi khu
      int count = Math.max(1, Math.min(MAX_ADD, AdminApi.intField(b, "count")));
      Float x = AdminApi.optFloat(b, "x");
      Float y = AdminApi.optFloat(b, "y");
      if (d.world().mapTemplate(map) == null) {
         throw new AdminApi.BadRequest("Không có map " + map + ".");
      }
      if ("plant".equals(kind)) {
         PlantConfig.Entry e = PlantConfig.current().entry(tpl);
         if (e == null || e.items().isEmpty()) {
            throw new AdminApi.BadRequest("Cây #" + tpl + " chưa có cấu hình hái (sửa ở Dữ liệu game → Cây hái được).");
         }
      } else {
         GatherConfig.Entry e = GatherConfig.current().entry(tpl);
         if (e == null) {
            throw new AdminApi.BadRequest("Không có cấu hình săn bắt cho quái #" + tpl + ".");
         }
         if (x == null && e.spots(map).isEmpty()) {
            throw new AdminApi.BadRequest("Map này không có điểm mọc " + kind + " trong cấu hình — nhập toạ độ X.");
         }
      }
      int placed = await(d.world().adminAddResource(kind, tpl, map, zone, x, y, count));
      if (placed == 0) {
         throw new AdminApi.BadRequest("Không đặt được (khu không có / map hết chỗ trống trên mặt đất).");
      }
      Log.info("Admin panel: dat them %d tai nguyen %s #%d map %d khu %d", placed, kind, tpl, map, zone);
      return AdminApi.ok("Đã đặt thêm " + placed + " tài nguyên ở " + d.mapName(map) + (zone >= 0 ? " khu " + zone
            : " (mọi khu)") + ". Hái / phá xong là hết, không mọc lại.");
   }
}
