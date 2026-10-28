package com.tlnt.server.admin.web;

import com.google.gson.JsonObject;
import com.tlnt.server.combat.MobRules;
import com.tlnt.server.item.ItemCodec;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.market.Listing;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.WorldPlayer;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Các tab quản lý máy chủ của admin panel:
 * <ul>
 *   <li><b>Máy chủ</b>: nhân EXP ({@code exp.rate}) đổi NGAY không cần khởi động lại (chỉ trong RAM, khởi động lại thì về
 *       server.properties), JVM (heap, GC, luồng), pool CSDL, dung lượng bảng, map / khu, xem
 *       server.properties (che mật khẩu / khoá).</li>
 *   <li><b>Bảo mật</b>: nhiều tài khoản cùng IP (đang online + theo lần đăng nhập cuối), kết nối theo IP, tài khoản đang
 *       bị khoá đăng nhập vì sai mật khẩu (gỡ khoá).</li>
 *   <li><b>Kí gửi</b>: danh sách đang bán (RAM là bản chuẩn), gỡ món về thư người bán.</li>
 *   <li><b>Bộ lạc</b>: danh sách + thành viên (chỉ xem).</li>
 *   <li><b>Tra chủ vật phẩm</b>: ai đang giữ mẫu item X (túi / rương / đồ mặc / bộ sưu tập / thư chưa nhận / kí gửi) —
 *       soi nhân bản đồ.</li>
 * </ul>
 */
final class AdminOps {

   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
         .withZone(ZoneId.systemDefault());
   private static final int MAX_OWNERS = 300;

   private final AdminApi.Deps d;

   AdminOps(AdminApi.Deps deps) {
      this.d = deps;
   }

   // ================================================================== máy chủ

   Object server() throws SQLException {
      Map<String, Object> out = AdminApi.ok(null);
      Map<String, Object> rt = new LinkedHashMap<>();
      rt.put("expRate", MobRules.expRate());
      out.put("runtime", rt);

      var mem = ManagementFactory.getMemoryMXBean();
      MemoryUsage heap = mem.getHeapMemoryUsage();
      MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();
      var threads = ManagementFactory.getThreadMXBean();
      var runtime = ManagementFactory.getRuntimeMXBean();
      var os = ManagementFactory.getOperatingSystemMXBean();
      Map<String, Object> jvm = new LinkedHashMap<>();
      jvm.put("heapUsedMb", heap.getUsed() >> 20);
      jvm.put("heapCommittedMb", heap.getCommitted() >> 20);
      jvm.put("heapMaxMb", heap.getMax() >> 20);
      jvm.put("nonHeapMb", nonHeap.getUsed() >> 20);
      jvm.put("threads", threads.getThreadCount());
      jvm.put("peakThreads", threads.getPeakThreadCount());
      jvm.put("daemonThreads", threads.getDaemonThreadCount());
      jvm.put("classes", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
      jvm.put("startedAt", TIME.format(Instant.ofEpochMilli(runtime.getStartTime())));
      jvm.put("uptimeSeconds", runtime.getUptime() / 1000);
      jvm.put("vm", runtime.getVmName() + " " + runtime.getVmVersion());
      jvm.put("os", os.getName() + " " + os.getVersion() + " · " + os.getArch());
      jvm.put("cores", os.getAvailableProcessors());
      jvm.put("load", os.getSystemLoadAverage());
      jvm.put("args", String.join(" ", runtime.getInputArguments()));
      List<Object[]> gcs = new ArrayList<>();
      for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
         gcs.add(new Object[] {gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()});
      }
      jvm.put("gc", gcs);
      out.put("jvm", jvm);

      int[] dp = d.dataDb().poolStats();
      int[] pp = d.playerDb().poolStats();
      out.put("pools", List.of(new Object[] {"tlnt_data (dữ liệu tĩnh)", dp[0], dp[1], dp[2], dp[3]},
            new Object[] {"tlnt_server (tài khoản, nhân vật)", pp[0], pp[1], pp[2], pp[3]}));
      out.put("pendingWrites", d.players().pendingWrites());

      out.put("tables", tableSizes());

      List<Map<String, Object>> maps = new ArrayList<>();
      for (Object[] m : d.world().mapStats()) {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", m[0]);
         row.put("name", m[1]);
         row.put("zones", m[2]);
         row.put("players", m[3]);
         maps.add(row);
      }
      maps.sort((a, b) -> Integer.compare((Integer) b.get("players"), (Integer) a.get("players")));
      out.put("maps", maps);
      out.put("config", d.config() == null ? Map.of() : d.config().masked());
      return out;
   }

   /** Đổi nhân EXP ({@code exp.rate}). Chỉ trong RAM — khởi động lại thì về server.properties. */
   /** Dung lượng bảng: information_schema khá nặng với MariaDB -> đọc tối đa 1 lần / {@link #TABLES_CACHE_MS}. */
   static final long TABLES_CACHE_MS = 5 * 60_000;
   private volatile List<Object[]> tablesCache;
   private volatile long tablesAt;

   private List<Object[]> tableSizes() throws SQLException {
      long now = System.currentTimeMillis();
      List<Object[]> cached = tablesCache;
      if (cached != null && now - tablesAt < TABLES_CACHE_MS) {
         return cached;
      }
      List<Object[]> tables = new ArrayList<>();
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT table_schema, table_name, table_rows,"
                  + " data_length + index_length FROM information_schema.tables WHERE table_schema IN"
                  + " (DATABASE(), 'tlnt_data') ORDER BY data_length + index_length DESC");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            tables.add(new Object[] {rs.getString(1) + "." + rs.getString(2), rs.getLong(3), rs.getLong(4)});
         }
      }
      tablesCache = tables;
      tablesAt = now;
      return tables;
   }

   Object setRuntime(JsonObject b) {
      StringBuilder log = new StringBuilder();
      if (b.has("expRate")) {
         int rate = b.get("expRate").getAsInt();
         if (rate < 1 || rate > 100) {
            throw new AdminApi.BadRequest("exp.rate 1-100.");
         }
         MobRules.setExpRate(rate);
         log.append(" exp.rate=").append(rate);
      }
      Log.info("Admin panel: doi thong so chay%s", log);
      return AdminApi.ok("Đã áp dụng ngay (chỉ đến lần khởi động lại; muốn giữ lâu dài hãy sửa server.properties).");
   }

   Object gc() {
      Runtime rt = Runtime.getRuntime();
      long before = (rt.totalMemory() - rt.freeMemory()) >> 20;
      System.gc();
      long after = (rt.totalMemory() - rt.freeMemory()) >> 20;
      Log.info("Admin panel: System.gc %d -> %d MB", before, after);
      return AdminApi.ok("Đã gọi dọn bộ nhớ: " + before + " MB → " + after + " MB.");
   }

   // ================================================================== bảo mật

   Object security() throws SQLException {
      Map<String, Object> out = AdminApi.ok(null);
      Map<String, List<Object[]>> byIp = new TreeMap<>();
      for (WorldPlayer p : d.world().onlinePlayers()) {
         PlayerData pd = p.data();
         if (pd.bot) {
            continue; // bot dùng chung "IP" ảo
         }
         byIp.computeIfAbsent(p.session().getRemoteIp(), k -> new ArrayList<>())
               .add(new Object[] {pd.getId(), pd.name, pd.level});
      }
      List<Map<String, Object>> shared = new ArrayList<>();
      byIp.forEach((ip, list) -> {
         if (list.size() > 1) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ip", ip);
            row.put("players", list);
            shared.add(row);
         }
      });
      shared.sort((a, b) -> Integer.compare(((List<?>) b.get("players")).size(), ((List<?>) a.get("players")).size()));
      out.put("onlineShared", shared);
      out.put("onlineIps", byIp.size());

      List<Object[]> lastIp = new ArrayList<>();
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT last_login_ip, COUNT(*), SUM(banned <> 0),"
                  + " SUBSTRING(GROUP_CONCAT(username ORDER BY last_login_at DESC SEPARATOR ', '), 1, 300)"
                  + " FROM account WHERE last_login_ip IS NOT NULL AND last_login_ip <> '' GROUP BY last_login_ip"
                  + " HAVING COUNT(*) > 1 ORDER BY COUNT(*) DESC LIMIT 50");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            lastIp.add(new Object[] {rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(4)});
         }
      }
      out.put("sharedLastIp", lastIp);
      var guard = d.accounts().guard();
      List<Object[]> conns = new ArrayList<>();
      guard.connectionsByIp(2).forEach((ip, n) -> conns.add(new Object[] {ip, n}));
      conns.sort((a, b) -> Integer.compare((Integer) b[1], (Integer) a[1]));
      out.put("connections", conns);
      out.put("maxConnectionsPerIp", guard.maxConnectionsPerIp());
      out.put("locked", guard.lockedAccounts(System.currentTimeMillis()));
      return out;
   }

   Object unlock(String username) {
      if (!d.accounts().guard().unlockAccount(username)) {
         throw new AdminApi.BadRequest("Tài khoản không bị khoá đăng nhập.");
      }
      Log.info("Admin panel: go khoa dang nhap '%s'", Log.ascii(username));
      return AdminApi.ok("Đã gỡ khoá đăng nhập cho " + username + ".");
   }

   // ================================================================== kí gửi

   Object market() {
      List<Map<String, Object>> list = new ArrayList<>();
      long now = System.currentTimeMillis();
      if (d.market() != null) {
         for (Listing l : d.market().adminList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", l.id());
            row.put("sellerId", l.sellerId());
            row.put("seller", l.sellerName());
            row.put("item", AdminItems.view(l.item(), -1));
            row.put("price", l.price());
            row.put("fee", l.fee());
            row.put("created", TIME.format(Instant.ofEpochMilli(l.createdAt())));
            row.put("expire", TIME.format(Instant.ofEpochMilli(l.expireAt())));
            row.put("secondsLeft", Math.max(0, (l.expireAt() - now) / 1000));
            list.add(row);
         }
      }
      list.sort((a, b) -> Integer.compare((Integer) b.get("id"), (Integer) a.get("id")));
      return list;
   }

   Object marketCancel(int id, String reason) {
      if (d.market() == null || !d.market().adminCancel(id, reason)) {
         throw new AdminApi.BadRequest("Món đã bán / đã gỡ.");
      }
      Log.info("Admin panel: go ki gui #%d", id);
      return AdminApi.ok("Đã gỡ món kí gửi #" + id + ", đồ về hòm thư người bán.");
   }

   // ================================================================== bộ lạc

   Object tribes() {
      return d.tribes() == null ? List.of() : d.tribes().adminList();
   }

   // ================================================================== tra chủ vật phẩm

   /**
    * Ai đang giữ mẫu item {@code templateId}: SQL lọc thô theo chuỗi {@code "i":N} trong các cột item rồi đọc JSON đếm
    * đúng; người đang online đếm trên RAM (mới hơn SQL). Kèm thư chưa nhận đồ + món đang kí gửi.
    */
   Object itemOwners(int templateId) throws SQLException {
      if (com.tlnt.server.data.GameDataCache.itemTemplate(templateId) == null) {
         throw new AdminApi.BadRequest("Không có mẫu item " + templateId + ".");
      }
      String a = "%\"i\":" + templateId + ",%";
      String b = "%\"i\":" + templateId + "}%";
      Map<Integer, long[]> counts = new LinkedHashMap<>(); // body, bag, box, collection, mail
      Map<Integer, String[]> names = new LinkedHashMap<>();
      String cols = "CONCAT_WS(' ', item_bag, item_box, item_body, item_back, item_costume, item_title, item_mask, item_pet)";
      try (Connection c = d.playerDb().getConnection()) {
         try (PreparedStatement ps = c.prepareStatement("SELECT id, name, level, bag_size, box_size, item_bag, item_box,"
               + " item_body, item_back, item_costume, item_title, item_mask, item_pet FROM player WHERE " + cols
               + " LIKE ? OR " + cols + " LIKE ? LIMIT 2000")) {
            ps.setString(1, a);
            ps.setString(2, b);
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  int id = rs.getInt(1);
                  if (d.world().online(id) != null) {
                     continue; // đếm trên RAM bên dưới
                  }
                  long[] cnt = new long[5];
                  cnt[0] += count(ItemContainer.fromJson(rs.getString(8), PlayerData.BODY_SLOTS).asList(), templateId);
                  cnt[1] += count(ItemContainer.fromJson(rs.getString(6), rs.getInt(4)).asList(), templateId);
                  cnt[2] += count(ItemContainer.fromJson(rs.getString(7), rs.getInt(5)).asList(), templateId);
                  ItemData back = ItemCodec.itemFromJson(rs.getString(9));
                  cnt[0] += back != null && back.templateId == templateId ? back.quantity : 0;
                  for (int k = 10; k <= 13; k++) {
                     ItemCollection col = ItemCodec.collectionFromJson(rs.getString(k));
                     cnt[3] += col == null ? 0 : count(col.items, templateId);
                  }
                  if (cnt[0] + cnt[1] + cnt[2] + cnt[3] > 0) {
                     counts.put(id, cnt);
                     names.put(id, new String[] {rs.getString(2), String.valueOf(rs.getInt(3)), "0"});
                  }
               }
            }
         }
         for (WorldPlayer wp : d.world().onlinePlayers()) {
            PlayerData p = wp.data();
            if (p.bot) {
               continue; // đồ của bot sinh ra rồi mất, không phải đồ thật
            }
            long[] cnt = new long[5];
            cnt[0] += count(p.body.asList(), templateId);
            cnt[1] += count(p.bag.asList(), templateId);
            cnt[2] += count(p.box.asList(), templateId);
            for (ItemCollection col : new ItemCollection[] {p.costumes, p.titles, p.masks, p.pets, p.backs}) {
               cnt[3] += col == null ? 0 : count(col.items, templateId);
            }
            if (cnt[0] + cnt[1] + cnt[2] + cnt[3] > 0) {
               counts.put(p.getId(), cnt);
               names.put(p.getId(), new String[] {p.name, String.valueOf(p.level), "1"});
            }
         }
         try (PreparedStatement ps = c.prepareStatement("SELECT m.owner_id, p.name, p.level, m.items FROM mail m"
               + " JOIN player p ON p.id = m.owner_id WHERE m.claimed = 0 AND (m.items LIKE ? OR m.items LIKE ?)")) {
            ps.setString(1, a);
            ps.setString(2, b);
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  int id = rs.getInt(1);
                  long n = count(MailRepository.parseItems(rs.getString(4)), templateId);
                  if (n > 0) {
                     counts.computeIfAbsent(id, k -> new long[5])[4] += n;
                     names.putIfAbsent(id, new String[] {rs.getString(2), String.valueOf(rs.getInt(3)),
                           d.world().online(id) != null ? "1" : "0"});
                  }
               }
            }
         }
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      long grand = 0;
      for (Map.Entry<Integer, long[]> e : counts.entrySet()) {
         long[] cnt = e.getValue();
         long total = cnt[0] + cnt[1] + cnt[2] + cnt[3] + cnt[4];
         grand += total;
         String[] n = names.get(e.getKey());
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", e.getKey());
         row.put("name", n[0]);
         row.put("level", Integer.parseInt(n[1]));
         row.put("online", "1".equals(n[2]));
         row.put("body", cnt[0]);
         row.put("bag", cnt[1]);
         row.put("box", cnt[2]);
         row.put("collection", cnt[3]);
         row.put("mail", cnt[4]);
         row.put("total", total);
         rows.add(row);
      }
      rows.sort((x, y) -> Long.compare((Long) y.get("total"), (Long) x.get("total")));
      List<Object[]> market = new ArrayList<>();
      if (d.market() != null) {
         for (Listing l : d.market().adminList()) {
            if (l.item().templateId == templateId) {
               market.add(new Object[] {l.id(), l.sellerName(), l.item().quantity, l.price()});
               grand += l.item().quantity;
            }
         }
      }
      Map<String, Object> out = AdminApi.ok(null);
      out.put("item", AdminItems.template(com.tlnt.server.data.GameDataCache.itemTemplate(templateId)));
      out.put("owners", rows.subList(0, Math.min(MAX_OWNERS, rows.size())));
      out.put("ownerCount", rows.size());
      out.put("market", market);
      out.put("total", grand);
      return out;
   }

   private static long count(List<ItemData> items, int templateId) {
      long n = 0;
      if (items != null) {
         for (ItemData it : items) {
            if (it != null && it.templateId == templateId) {
               n += it.quantity;
            }
         }
      }
      return n;
   }
}
