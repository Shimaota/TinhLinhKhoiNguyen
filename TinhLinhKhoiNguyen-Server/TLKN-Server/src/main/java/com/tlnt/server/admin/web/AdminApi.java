package com.tlnt.server.admin.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.account.AccountService;
import com.tlnt.server.admin.ShutdownService;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.db.Database;
import com.tlnt.server.giftcode.GiftcodeService;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.tournament.TournamentService;
import com.tlnt.server.tribe.TribeService;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;
import com.tlnt.server.worldboss.WorldBossService;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Các chức năng của admin panel ({@link AdminServer} gọi trên luồng "admin" — được chạy SQL). Đụng nhân vật đang online
 * thì xếp việc vào hàng đợi session của người đó (như gói tin) — {@link AdminPlayers}; tài khoản — {@link AdminAccounts};
 * hiển thị / sửa item — {@link AdminItems}.
 *
 * <pre>
 * GET  status | overview | maps | logs?after=
 *      accounts?q=&amp;filter=&amp;page= | account?id=
 *      players/online | players/search?q= | player?id=
 *      items?q= | stat-types | buffs
 *      broadcasts | tournaments | bosses | boss?id= | giftcodes
 *      server | security | market | tribes | item-owners?id=   ({@link AdminOps})
 *      boss-damage?id= | gather-locations | data/tables | data/rows?table=   ({@link AdminData})
 * POST account/create | account/password | account/email | account/ban | account/role | account/secondary | account/delete
 *      player/kick | player/level | player/currency | player/item/add | player/item/edit | player/item/remove
 *      player/buff/add | player/buff/remove | player/gift
 *      broadcast {text, repeat, interval} | broadcast/cancel {id} | maintenance {on} | shutdown {seconds}
 *      shutdown/cancel | tournament/open | tournament/skip | tournament/cancel | boss/spawn | boss/kill | boss/save
 *      giftcode/save | giftcode/delete | reload | server/runtime | server/gc | security/unlock | market/cancel
 *      boss/damage {id, player, damage} | data/save {table, key?, row} | data/delete {table, key}
 *      foxes | resource-types | player/progress?id=                                  ({@link AdminWorld}, {@link AdminPlayerProgress})
 *      fox/kill | fox/spawn | fox/refund | boss/move | resource/remove | resource/add
 *      player/progress/action
 * </pre>
 */
public final class AdminApi {

   /** Lỗi dữ liệu gửi lên -> HTTP 400 kèm câu báo. */
   public static final class BadRequest extends RuntimeException {
      public BadRequest(String message) {
         super(message);
      }
   }

   /** Mọi thành phần server admin panel cần (GameServer nối lúc khởi động). */
   public record Deps(World world, AccountService accounts, PlayerService players, MailService mails,
         ItemService items, RankingService rankings, TribeService tribes,
         com.tlnt.server.market.MarketService market, com.tlnt.server.config.ServerConfig config,
         TournamentService tournaments,
         WorldBossService bosses, com.tlnt.server.worldboss.FoxService foxes,
         com.tlnt.server.achievement.AchievementService achievements, com.tlnt.server.daily.DailyQuestService dailyQuests,
         GiftcodeService giftcodes, Database dataDb, Database playerDb,
         IntSupplier connections, IntSupplier sessions, Runnable reloadAll, Supplier<ShutdownService> shutdown,
         ScheduledExecutorService scheduler, Executor gamePool) {

      String mapName(int mapId) {
         var t = world.mapTemplate(mapId);
         return t == null ? "#" + mapId : t.name();
      }
   }

   static final long SAMPLE_SECONDS = 10;
   /** Số liệu tổng quan (vài câu GROUP BY trên bảng player / account) tính lại tối đa 1 lần / khoảng này. */
   static final long OVERVIEW_CACHE_MS = 30_000;
   private static final int MAX_BROADCAST_HISTORY = 30;
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
   private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

   private final Deps d;
   private final AdminAccounts accounts;
   private final AdminPlayers players;
   private final AdminOps ops;
   private final AdminData data;
   private final AdminWorld worldOps;
   private final AdminPlayerProgress playerProgress;
   private final long startedAt = System.currentTimeMillis();
   private ScheduledFuture<?> shutdownTask;
   private long shutdownAt;
   /** Đỉnh online: cả lúc chạy + trong ngày (lấy mẫu mỗi {@link #SAMPLE_SECONDS} s). */
   private int peakOnline;
   private String peakAt = "";
   private int peakToday;
   private String peakTodayAt = "";
   private LocalDate peakDay = LocalDate.now();
   private Map<String, Object> overviewCache;
   private long overviewAt;
   private Map<String, Object> statusCache;
   private long statusCacheAt;
   /** Thông báo đã phát (mới nhất trước). */
   private final ArrayDeque<Broadcast> broadcasts = new ArrayDeque<>();
   private final AtomicInteger broadcastIds = new AtomicInteger();

   /** 1 lần phát thông báo (có thể lặp lại). */
   private static final class Broadcast {
      final int id;
      final String text;
      final String time;
      final int repeat;
      final int interval;
      int sent;
      ScheduledFuture<?> task;
      boolean cancelled;

      Broadcast(int id, String text, int repeat, int interval) {
         this.id = id;
         this.text = text;
         this.time = LocalDateTime.now().format(DATE);
         this.repeat = repeat;
         this.interval = interval;
      }
   }

   public AdminApi(Deps deps) {
      this.d = deps;
      this.accounts = new AdminAccounts(deps);
      this.players = new AdminPlayers(deps);
      this.ops = new AdminOps(deps);
      this.data = new AdminData(deps);
      this.worldOps = new AdminWorld(deps, players);
      this.playerProgress = new AdminPlayerProgress(deps, players);
      deps.scheduler().scheduleAtFixedRate(this::sample, SAMPLE_SECONDS, SAMPLE_SECONDS, TimeUnit.SECONDS);
   }

   /** @return đối tượng trả về dạng JSON; null = không có API này */
   Object handle(String method, String route, Map<String, String> q, JsonObject b) throws SQLException {
      if ("POST".equals(method)) {
         return action(route, b);
      }
      return switch (route) {
         case "status" -> status();
         case "overview" -> overview("1".equals(q.get("fresh")));
         case "maps" -> maps();
         case "logs" -> Log.recent(q.containsKey("after") ? Long.parseLong(q.get("after")) : 0);
         case "accounts" -> accounts.list(q.get("q"), q.get("filter"), parseInt(q.getOrDefault("page", "0"), 0));
         case "account" -> accounts.detail(intOf(q.get("id"), "id"));
         case "players/online" -> players.online(q.get("q"));
         case "players/search" -> players.search(q.getOrDefault("q", ""));
         case "player" -> players.detail(intOf(q.get("id"), "id"));
         case "player/item" -> players.itemView(intOf(q.get("id"), "id"), q.getOrDefault("box", ""),
               intOf(q.get("slot"), "slot"));
         case "items" -> AdminItems.search(q.getOrDefault("q", ""), 100);
         case "stat-types" -> AdminItems.statTypes();
         case "soul-charms" -> AdminPlayers.soulCharmList();
         case "element-stones" -> AdminPlayers.elementStoneList();
         case "buffs" -> AdminPlayers.buffList();
         case "broadcasts" -> broadcastList();
         case "tournaments" -> d.tournaments().adminList();
         case "bosses" -> bosses();
         case "boss" -> boss(intOf(q.get("id"), "id"));
         case "boss-damage" -> bossDamage(intOf(q.get("id"), "id"), 0, 0);
         case "gather-locations" -> gatherLocations();
         case "giftcodes" -> giftcodes();
         case "events" -> events();
         case "server" -> ops.server();
         case "security" -> ops.security();
         case "market" -> ops.market();
         case "tribes" -> ops.tribes();
         case "item-owners" -> ops.itemOwners(intOf(q.get("id"), "id"));
         case "map-list" -> players.mapList();
         case "data/tables" -> data.tables();
         case "foxes" -> worldOps.foxes();
         case "resource-types" -> worldOps.resourceTypes();
         case "player/progress" -> playerProgress.view(intOf(q.get("id"), "id"));
         case "data/rows" -> data.rows(q.getOrDefault("table", ""));
         case "bots" -> AdminBots.view(q.get("kind"), q.get("q"));
         case "bots/detail" -> AdminBots.detail(intOf(q.get("id"), "id"));
         default -> null;
      };
   }

   private Object action(String route, JsonObject b) throws SQLException {
      return switch (route) {
         case "account/create" -> accounts.create(b);
         case "account/password" -> accounts.password(intField(b, "id"), str(b, "password"));
         case "account/email" -> accounts.email(intField(b, "id"), str(b, "email"));
         case "account/ban" -> accounts.ban(intField(b, "id"), bool(b, "banned"), str(b, "reason"));
         case "account/role" -> accounts.setRole(intField(b, "id"), intField(b, "role"));
         case "account/secondary" -> accounts.secondary(intField(b, "id"), str(b, "action"), str(b, "password"));
         case "account/delete" -> accounts.delete(intField(b, "id"));
         case "player/kick" -> players.kick(intField(b, "id"));
         case "player/level" -> players.level(intField(b, "id"), intField(b, "level"));
         case "player/currency" -> players.currency(intField(b, "id"), b);
         case "player/item/add" -> players.itemAdd(intField(b, "id"), str(b, "box"), obj(b, "item"));
         case "player/item/edit" -> players.itemEdit(intField(b, "id"), str(b, "box"), intField(b, "slot"),
               obj(b, "item"));
         case "player/item/enhance" -> players.itemEnhance(intField(b, "id"), intField(b, "slot"), b);
         case "player/item/remove" -> players.itemRemove(intField(b, "id"), str(b, "box"), intField(b, "slot"),
               intField(b, "qty"));
         case "player/buff/add" -> players.buffAdd(intField(b, "id"), intField(b, "item"), num(b, "minutes"));
         case "player/buff/remove" -> players.buffRemove(intField(b, "id"), intField(b, "option"));
         case "player/item/action" -> players.itemAction(intField(b, "id"), str(b, "box"), intField(b, "slot"),
               str(b, "action"), b);
         case "player/stats" -> players.editStats(intField(b, "id"), b);
         case "player/skill" -> players.skillLevel(intField(b, "id"), intField(b, "skill"), intField(b, "level"));
         case "player/teleport" -> players.teleport(intField(b, "id"), intField(b, "map"), optFloat(b, "x"), optFloat(b, "y"));
         case "tribe/update" -> done2(d.tribes().adminUpdate(intField(b, "id"), optStr(b, "name"), optStr(b, "slogan"),
               b.has("level") ? intField(b, "level") : null, optLong(b, "exp"), optLong(b, "gold"), optLong(b, "diamond")),
               "Đã lưu bộ lạc.");
         case "tribe/kick" -> done2(d.tribes().adminKick(intField(b, "id"), intField(b, "player")), "Đã đưa ra khỏi bộ lạc.");
         case "tribe/role" -> done2(d.tribes().adminSetRole(intField(b, "id"), intField(b, "player"), intField(b, "role")),
               "Đã đổi chức vụ.");
         case "player/gift" -> players.gift(intField(b, "id"), arr(b, "items"), str(b, "title"), str(b, "content"));
         case "broadcast" -> broadcast(str(b, "text"), intField(b, "repeat"), intField(b, "interval"));
         case "broadcast/cancel" -> cancelBroadcast(intField(b, "id"));
         case "maintenance" -> maintenance(bool(b, "on"));
         case "shutdown" -> shutdown(intField(b, "seconds"));
         case "shutdown/cancel" -> cancelShutdown();
         case "tournament/open" -> done(d.tournaments().adminOpen(str(b, "kind"), intField(b, "seconds")));
         case "tournament/skip" -> {
            d.tournaments().adminSkip(str(b, "kind"));
            yield ok("Đã chuyển giai đoạn.");
         }
         case "tournament/cancel" -> {
            d.tournaments().adminCancel(str(b, "kind"));
            yield ok("Đã huỷ giải.");
         }
         case "boss/spawn" -> done(d.bosses().adminSpawn(intField(b, "id")));
         case "boss/kill" -> {
            d.bosses().adminKill(intField(b, "id"));
            yield ok("Đã hạ boss (nếu đang sống).");
         }
         case "boss/save" -> saveBoss(b);
         case "boss/damage" -> {
            int player = intField(b, "player");
            if (player <= 0) {
               throw new BadRequest("Thiếu người chơi.");
            }
            long dmg = Math.max(0, optLong(b, "damage") == null ? 0 : optLong(b, "damage"));
            bossDamage(intField(b, "id"), player, dmg);
            Log.info("Admin panel: sua sat thuong boss #%d nguoi %d = %d", intField(b, "id"), player, dmg);
            yield ok(dmg <= 0 ? "Đã xoá người chơi khỏi bảng sát thương." : "Đã đặt sát thương = " + dmg + ".");
         }
         case "server/runtime" -> ops.setRuntime(b);
         case "server/gc" -> ops.gc();
         case "security/unlock" -> ops.unlock(str(b, "username"));
         case "market/cancel" -> ops.marketCancel(intField(b, "id"), str(b, "reason"));
         case "giftcode/save" -> saveGiftcode(b);
         case "event/set" -> setEvent(str(b, "key"), bool(b, "on"));
         case "event/schedule" -> scheduleEvent(str(b, "key"), optLong(b, "start"), optLong(b, "end"));
         case "event/rabbits" -> com.tlnt.server.event.RabbitService.adminRespawnAll()
               ? ok("Đã gọi lại 5 Ngọc thố: các con mọc lại ở map ngẫu nhiên trong vài giây.")
               : done("Sự kiện Trung thu đang tắt.");
         case "data/save" -> data.save(b);
         case "fox/kill" -> worldOps.foxKill(intField(b, "slot"));
         case "fox/spawn" -> worldOps.foxSpawn();
         case "fox/refund" -> worldOps.foxRefund(intField(b, "slot"));
         case "boss/move" -> worldOps.moveBoss(b);
         case "resource/remove" -> worldOps.resourceRemove(b);
         case "resource/add" -> worldOps.resourceAdd(b);
         case "player/progress/action" -> playerProgress.action(intField(b, "id"), str(b, "action"), b);
         case "data/delete" -> data.delete(b);
         case "bots/config" -> AdminBots.save(b);
         case "bots/kick" -> AdminBots.kick(intField(b, "id"));
         case "bots/kick-all" -> AdminBots.kickAll();
         case "giftcode/delete" -> deleteGiftcode(str(b, "code"));
         case "reload" -> {
            d.reloadAll().run();
            Log.info("Admin panel: nap lai du lieu cau hinh");
            yield ok("Đã nạp lại dữ liệu cấu hình (shop, giftcode, phúc lợi, boss, câu cá, ...).");
         }
         default -> null;
      };
   }

   // ================================================================== giám sát

   private Map<String, Object> status() {
      long now = System.currentTimeMillis();
      synchronized (this) {
         if (statusCache != null && now - statusCacheAt < 2500) {
            return new LinkedHashMap<>(statusCache);
         }
      }
      Runtime rt = Runtime.getRuntime();
      Map<String, Object> out = ok(null);
      out.put("uptimeSeconds", (now - startedAt) / 1000);
      out.put("online", d.accounts().onlineCount());
      int bots = AdminBots.online();
      out.put("inWorld", Math.max(0, d.world().onlineCount() - bots)); // người thật
      out.put("bots", bots);
      out.put("connections", d.connections().getAsInt());
      out.put("sessions", d.sessions().getAsInt());
      out.put("pendingWrites", d.players().pendingWrites());
      out.put("ramUsedMb", (rt.totalMemory() - rt.freeMemory()) >> 20);
      out.put("ramMaxMb", rt.maxMemory() >> 20);
      out.put("cpuProcess", cpu(true));
      out.put("cpuSystem", cpu(false));
      out.put("cores", rt.availableProcessors());
      out.put("threads", ManagementFactory.getThreadMXBean().getThreadCount());
      int zones = 0;
      int activeMaps = 0;
      for (Object[] m : d.world().mapStats()) {
         zones += (Integer) m[2];
         if ((Integer) m[3] > 0) {
            activeMaps++;
         }
      }
      out.put("zones", zones);
      out.put("activeMaps", activeMaps);
      out.put("maintenance", d.accounts().isMaintenance());
      out.put("shutdownInSeconds", shutdownTask == null ? -1 : Math.max(0, (shutdownAt - now) / 1000));
      out.put("javaVersion", System.getProperty("java.version"));
      synchronized (this) {
         out.put("peakOnline", peakOnline);
         out.put("peakAt", peakAt);
         out.put("peakToday", peakToday);
         out.put("peakTodayAt", peakTodayAt);
         statusCache = out;
         statusCacheAt = now;
      }
      return out;
   }

   /** CPU % (0-100) của tiến trình / cả máy; -1 nếu JVM không cho đọc. */
   private static int cpu(boolean process) {
      java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
      if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
         double v = process ? sun.getProcessCpuLoad() : sun.getCpuLoad();
         return v < 0 ? -1 : (int) Math.round(v * 100);
      }
      return -1;
   }

   /** Lấy mẫu đỉnh online. */
   private synchronized void sample() {
      try {
         int online = d.world().onlineCount();
         LocalDateTime now = LocalDateTime.now();
         if (!now.toLocalDate().equals(peakDay)) {
            peakDay = now.toLocalDate();
            peakToday = 0;
            peakTodayAt = "";
         }
         if (online > peakOnline) {
            peakOnline = online;
            peakAt = now.format(DATE);
         }
         if (online > peakToday) {
            peakToday = online;
            peakTodayAt = now.format(HOUR);
         }
      } catch (Exception e) {
         Log.warn("Admin panel: loi lay mau thong ke: %s", e.getMessage());
      }
   }

   /**
    * Tổng quan: tổng tài khoản / nhân vật, online, hoạt động 24 giờ / 7 / 30 ngày, kinh tế. SQL cache {@link #OVERVIEW_CACHE_MS}; số online tính mỗi lần (RAM).
    */
   private Object overview(boolean fresh) throws SQLException {
      Map<String, Object> db;
      synchronized (this) {
         db = overviewCache;
      }
      long now = System.currentTimeMillis();
      if (db == null || fresh || now - overviewAt > OVERVIEW_CACHE_MS) {
         db = overviewSql();
         synchronized (this) {
            overviewCache = db;
            overviewAt = now;
         }
      }
      Map<String, Object> out = ok(null);
      out.putAll(db);
      out.put("computedAgo", (now - overviewAt) / 1000);
      // online trong RAM
      long levelSum = 0;
      List<WorldPlayer> online = d.world().onlinePlayers();
      online.removeIf(p -> p.data().bot); // người chơi giả không tính
      for (WorldPlayer p : online) {
         levelSum += p.data().level;
      }
      Map<String, Object> on = new LinkedHashMap<>();
      on.put("bots", AdminBots.online());
      on.put("count", online.size());
      on.put("avgLevel", online.isEmpty() ? 0 : Math.round(levelSum * 10.0 / online.size()) / 10.0);
      out.put("online", on);
      return out;
   }

   private Map<String, Object> overviewSql() throws SQLException {
      Map<String, Object> out = new LinkedHashMap<>();
      try (Connection c = d.playerDb().getConnection()) {
         Map<String, Object> totals = new LinkedHashMap<>();
         try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*), SUM(banned <> 0), SUM(role > 0),"
               + " SUM(secondary_password_hash IS NOT NULL), SUM(created_at >= CURDATE()),"
               + " SUM(created_at >= NOW() - INTERVAL 7 DAY) FROM account");
               ResultSet rs = ps.executeQuery()) {
            rs.next();
            totals.put("accounts", rs.getLong(1));
            totals.put("banned", rs.getLong(2));
            totals.put("staff", rs.getLong(3));
            totals.put("pw2", rs.getLong(4));
            totals.put("newAccountsToday", rs.getLong(5));
            totals.put("newAccounts7d", rs.getLong(6));
         }
         try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*), AVG(level), MAX(level),"
               + " SUM(last_login_at >= NOW() - INTERVAL 1 DAY), SUM(last_login_at >= NOW() - INTERVAL 7 DAY),"
               + " SUM(last_login_at >= NOW() - INTERVAL 30 DAY), SUM(gold), SUM(locked_gold), SUM(diamond),"
               + " SUM(recharge_total), SUM(play_time_seconds), SUM(created_at >= CURDATE()) FROM player");
               ResultSet rs = ps.executeQuery()) {
            rs.next();
            totals.put("characters", rs.getLong(1));
            totals.put("avgLevel", Math.round(rs.getDouble(2) * 10) / 10.0);
            totals.put("maxLevel", rs.getInt(3));
            totals.put("active1d", rs.getLong(4));
            totals.put("active7d", rs.getLong(5));
            totals.put("active30d", rs.getLong(6));
            totals.put("gold", rs.getLong(7));
            totals.put("lockedGold", rs.getLong(8));
            totals.put("diamond", rs.getLong(9));
            totals.put("recharge", rs.getLong(10));
            long chars = Math.max(1, rs.getLong(1));
            totals.put("avgPlayHours", Math.round(rs.getLong(11) / 3600.0 / chars * 10) / 10.0);
            totals.put("newCharsToday", rs.getLong(12));
         }
         out.put("totals", totals);

      }
      return out;
   }

   private Object maps() {
      List<Map<String, Object>> list = new ArrayList<>();
      for (Object[] m : d.world().mapStats()) {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", m[0]);
         row.put("name", m[1]);
         row.put("zones", m[2]);
         row.put("players", m[3]);
         list.add(row);
      }
      return list;
   }

   /**
    * Vị trí cây / tổ ong / quả dừa đang có ở các khu thường + đồ nhặt được của từng loại ({@code items}: id, tên, icon —
    * lấy từ cấu hình đang chạy PlantConfig / GatherConfig) để trang lọc + hiện icon.
    */
   private Object gatherLocations() {
      List<Map<String, Object>> rows = d.world().adminGatherLocations();
      Map<String, List<Map<String, Object>>> cache = new java.util.HashMap<>();
      for (Map<String, Object> row : rows) {
         String kind = (String) row.get("kind");
         int tpl = (Integer) row.get("templateId");
         row.put("items", cache.computeIfAbsent(kind + tpl, k -> {
            List<RewardItem> src;
            if ("plant".equals(kind)) {
               var e = com.tlnt.server.plant.PlantConfig.current().entry(tpl);
               src = e == null ? List.of() : e.items();
            } else {
               var e = com.tlnt.server.gather.GatherConfig.current().entry(tpl);
               src = e == null ? List.of() : e.items();
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (RewardItem r : src) {
               var t = GameDataCache.itemTemplate(r.templateId());
               Map<String, Object> it = new LinkedHashMap<>();
               it.put("id", r.templateId());
               it.put("name", t == null ? "#" + r.templateId() : t.name());
               it.put("icon", AdminItems.icon(t));
               items.add(it);
            }
            return items;
         }));
      }
      return rows;
   }

   // ================================================================== thông báo + bảo trì

   /** Chữ chạy toàn server; {@code repeat} > 1 thì phát lại mỗi {@code interval} giây. */
   private Object broadcast(String text, int repeat, int interval) {
      String t = text == null ? "" : text.trim();
      if (t.isEmpty()) {
         throw new BadRequest("Nhập nội dung thông báo.");
      }
      if (t.length() > 300) {
         throw new BadRequest("Thông báo tối đa 300 ký tự.");
      }
      int times = Math.max(1, Math.min(50, repeat));
      int every = times > 1 ? Math.max(10, Math.min(3600, interval)) : 0;
      Broadcast bc = new Broadcast(broadcastIds.incrementAndGet(), t, times, every);
      synchronized (broadcasts) {
         broadcasts.addFirst(bc);
         while (broadcasts.size() > MAX_BROADCAST_HISTORY) {
            Broadcast old = broadcasts.pollLast();
            if (old.task != null) {
               old.task.cancel(false);
            }
         }
      }
      if (times == 1) {
         d.world().broadcastTicker(t);
         bc.sent = 1;
      } else {
         bc.task = d.scheduler().scheduleAtFixedRate(() -> {
            synchronized (broadcasts) {
               if (bc.cancelled || bc.sent >= bc.repeat) {
                  bc.task.cancel(false);
                  return;
               }
               bc.sent++;
            }
            d.world().broadcastTicker(t);
         }, 0, every, TimeUnit.SECONDS);
      }
      Log.info("Admin panel: thong bao toan server x%d: %s", times, Log.ascii(t));
      return ok(times == 1 ? "Đã phát thông báo cho " + d.world().onlineCount() + " người online."
            : "Đã hẹn phát " + times + " lần, mỗi " + every + " giây.");
   }

   private Object cancelBroadcast(int id) {
      synchronized (broadcasts) {
         for (Broadcast bc : broadcasts) {
            if (bc.id == id && bc.task != null && !bc.cancelled && bc.sent < bc.repeat) {
               bc.cancelled = true;
               bc.task.cancel(false);
               return ok("Đã dừng lịch phát.");
            }
         }
      }
      throw new BadRequest("Thông báo không còn lịch phát.");
   }

   private Object broadcastList() {
      List<Map<String, Object>> list = new ArrayList<>();
      synchronized (broadcasts) {
         for (Broadcast bc : broadcasts) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", bc.id);
            row.put("text", bc.text);
            row.put("time", bc.time);
            row.put("repeat", bc.repeat);
            row.put("interval", bc.interval);
            row.put("sent", bc.sent);
            row.put("running", bc.task != null && !bc.cancelled && bc.sent < bc.repeat);
            row.put("cancelled", bc.cancelled);
            list.add(row);
         }
      }
      return list;
   }

   /** Danh sách sự kiện + trạng thái boss của sự kiện (dòng boss thế giới có id = "boss"). */
   private Object events() throws SQLException {
      List<Map<String, Object>> list = com.tlnt.server.event.GameEvents.adminList();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> bosses = (List<Map<String, Object>>) bosses();
      for (Map<String, Object> row : list) {
         Object bossId = row.get("boss");
         for (Map<String, Object> boss : bosses) {
            if (bossId != null && bossId.equals(boss.get("id"))) {
               row.put("bossInfo", boss);
            }
         }
      }
      return list;
   }

   /** Lịch tự bật / tắt sự kiện (epoch ms, null / 0 = bỏ mốc). */
   private Object scheduleEvent(String key, Long start, Long end) throws SQLException {
      com.tlnt.server.event.GameEvents.Event e = com.tlnt.server.event.GameEvents.Event.of(key);
      if (e == null) {
         throw new BadRequest("Không có sự kiện " + key + ".");
      }
      long s = start == null ? 0 : start;
      long t = end == null ? 0 : end;
      if (s > 0 && t > 0 && t <= s) {
         throw new BadRequest("Giờ kết thúc phải sau giờ bắt đầu.");
      }
      com.tlnt.server.event.GameEvents.setSchedule(e, s, t);
      Log.info("Admin panel: lich su kien %s bat %d tat %d", e.key, s, t);
      return ok(s == 0 && t == 0 ? "Đã bỏ lịch tự bật / tắt." : "Đã lưu lịch tự bật / tắt sự kiện " + e.title + ".");
   }

   /** Bật / tắt sự kiện (SQL server_state + báo các hệ thống). */
   private Object setEvent(String key, boolean on) throws SQLException {
      com.tlnt.server.event.GameEvents.Event e = com.tlnt.server.event.GameEvents.Event.of(key);
      if (e == null) {
         throw new BadRequest("Không có sự kiện " + key + ".");
      }
      boolean changed = com.tlnt.server.event.GameEvents.set(e, on);
      Log.info("Admin panel: su kien %s %s", e.key, on ? "BAT" : "TAT");
      return ok(!changed ? "Sự kiện " + e.title + " vốn đã " + (on ? "bật." : "tắt.")
            : on ? "Đã bật sự kiện " + e.title + "." : "Đã tắt sự kiện " + e.title + ": mọi thứ của sự kiện đã gỡ.");
   }

   private Object maintenance(boolean on) {
      d.accounts().setMaintenance(on);
      Log.info("Admin panel: bao tri %s", on ? "BAT (chan dang nhap moi)" : "TAT");
      return ok(on ? "Đã bật bảo trì: chặn đăng nhập mới (người đang chơi không bị ảnh hưởng)."
            : "Đã tắt bảo trì: cho đăng nhập lại.");
   }

   /** Tắt server an toàn sau {@code seconds} giây, chữ chạy đếm ngược toàn server. */
   private synchronized Object shutdown(int seconds) {
      if (seconds < 0 || seconds > 3600) {
         throw new BadRequest("Thời gian đếm ngược 0-3600 giây.");
      }
      cancelTask();
      shutdownAt = System.currentTimeMillis() + seconds * 1000L;
      shutdownTask = d.scheduler().scheduleAtFixedRate(this::shutdownTick, 0, 1, TimeUnit.SECONDS);
      Log.info("Admin panel: hen tat server sau %d giay", seconds);
      return ok("Server sẽ tắt sau " + seconds + " giây.");
   }

   private synchronized void shutdownTick() {
      long left = (shutdownAt - System.currentTimeMillis() + 500) / 1000;
      if (left <= 0) {
         cancelTask();
         d.world().broadcastTicker("Máy chủ bắt đầu bảo trì, hẹn gặp lại!");
         Thread t = new Thread(() -> {
            d.shutdown().get().shutdown();
            System.exit(0);
         }, "admin-shutdown");
         t.start();
         return;
      }
      if (left == 600 || left == 300 || left == 180 || left == 120 || left == 60 || left == 30 || left == 10
            || left <= 5) {
         d.world().broadcastTicker("Máy chủ sẽ bảo trì sau " + (left >= 60 ? left / 60 + " phút" : left + " giây")
               + ", hãy thoát game để tránh mất dữ liệu.");
      }
   }

   private synchronized Object cancelShutdown() {
      if (shutdownTask == null) {
         throw new BadRequest("Không có lịch tắt server.");
      }
      cancelTask();
      d.world().broadcastTicker("Đã huỷ lịch bảo trì máy chủ.");
      Log.info("Admin panel: huy hen tat server");
      return ok("Đã huỷ lịch tắt server.");
   }

   private void cancelTask() {
      if (shutdownTask != null) {
         shutdownTask.cancel(false);
         shutdownTask = null;
      }
   }

   // ================================================================== boss thế giới (tlnt_data.world_boss)

   /** Danh sách boss + HP đang có (hỏi luồng tick khu của boss đang sống, chờ tối đa 500 ms / boss). */
   private Object bosses() throws SQLException {
      List<Map<String, Object>> list = d.bosses().adminList();
      com.tlnt.server.bot.BotService bots = com.tlnt.server.bot.BotService.instance();
      for (Map<String, Object> row : list) {
         row.put("hunters", bots == null ? 0 : bots.huntersOf((Integer) row.get("id")));
         if (Boolean.TRUE.equals(row.get("alive"))) {
            try {
               long[] hp = d.world().worldBossHp((Integer) row.get("mapId"), (Integer) row.get("zoneId"),
                     (Integer) row.get("id")).get(500, TimeUnit.MILLISECONDS);
               if (hp != null) {
                  row.put("hp", hp[0]);
                  row.put("maxHp", hp[1]);
               }
            } catch (Exception e) {
               row.put("hp", -1);
            }
         }
      }
      // boss đang tắt (active = 0) vẫn hiện để bật / sửa
      java.util.Set<Integer> shown = new java.util.HashSet<>();
      list.forEach(r -> shown.add((Integer) r.get("id")));
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT id, name FROM world_boss WHERE active = 0 ORDER BY id");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            if (shown.add(rs.getInt(1))) {
               Map<String, Object> row = new LinkedHashMap<>();
               row.put("id", rs.getInt(1));
               row.put("name", rs.getString(2));
               row.put("alive", false);
               row.put("disabled", true);
               row.put("status", "Đang tắt");
               list.add(row);
            }
         }
      }
      return list;
   }

   /**
    * Bảng sát thương lên boss đang sống (đọc trên luồng tick khu, chờ tối đa 1 s); {@code editPlayer} > 0 thì đặt sát
    * thương người đó = {@code damage} (0 = xoá khỏi sổ) trước khi đọc.
    */
   private Object bossDamage(int id, int editPlayer, long damage) {
      Map<String, Object> row = null;
      for (Map<String, Object> r : d.bosses().adminList()) {
         if ((Integer) r.get("id") == id) {
            row = r;
         }
      }
      if (row == null) {
         throw new BadRequest("Không có boss #" + id + ".");
      }
      Map<String, Object> out = ok(null);
      out.put("id", id);
      out.put("name", row.get("name"));
      out.put("alive", row.get("alive"));
      out.put("status", row.get("status"));
      List<Map<String, Object>> list = new ArrayList<>();
      if (Boolean.TRUE.equals(row.get("alive"))) {
         List<Object[]> rows;
         try {
            rows = d.world().worldBossDamage((Integer) row.get("mapId"), (Integer) row.get("zoneId"), id, editPlayer,
                  damage).get(1, TimeUnit.SECONDS);
         } catch (Exception e) {
            throw new BadRequest("Khu của boss không trả lời, thử lại.");
         }
         if (rows == null && editPlayer > 0) {
            throw new BadRequest("Boss đã chết / biến mất.");
         }
         long total = 0;
         if (rows != null) {
            for (Object[] r : rows) {
               total += (Long) r[2];
            }
            int rank = 0;
            for (Object[] r : rows) {
               Map<String, Object> m = new LinkedHashMap<>();
               m.put("rank", ++rank);
               m.put("id", r[0]);
               m.put("name", r[1]);
               m.put("damage", r[2]);
               m.put("inZone", r[3]);
               m.put("online", d.world().online((Integer) r[0]) != null);
               m.put("percent", total > 0 ? Math.round((Long) r[2] * 1000.0 / total) / 10.0 : 0);
               list.add(m);
            }
         }
         out.put("total", total);
      } else if (editPlayer > 0) {
         throw new BadRequest("Boss không còn sống.");
      }
      out.put("rows", list);
      return out;
   }

   /** 1 dòng world_boss để sửa: tên, bật, map, giờ, chỉ số chính trong spec, quà (mảng). */
   private Object boss(int id) throws SQLException {
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT id, name, active, maps, respawn_minutes, first_minutes,"
                  + " spec, loot FROM world_boss WHERE id = ?")) {
         ps.setInt(1, id);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               throw new BadRequest("Không có boss #" + id + ".");
            }
            Map<String, Object> out = ok(null);
            out.put("id", rs.getInt(1));
            out.put("name", rs.getString(2));
            out.put("active", rs.getBoolean(3));
            List<Map<String, Object>> maps = new ArrayList<>();
            for (JsonElement e : JsonParser.parseString(rs.getString(4)).getAsJsonArray()) {
               Map<String, Object> m = new LinkedHashMap<>();
               m.put("id", e.getAsInt());
               m.put("name", d.mapName(e.getAsInt()));
               maps.add(m);
            }
            out.put("maps", maps);
            out.put("respawnMinutes", rs.getInt(5));
            out.put("firstMinutes", rs.getInt(6));
            JsonObject spec = JsonParser.parseString(rs.getString(7)).getAsJsonObject();
            out.put("kind", spec.has("char") ? "Boss người chơi giả" : "Boss quái");
            out.put("level", spec.has("level") ? spec.get("level").getAsInt() : 1);
            out.put("hp", spec.has("hp") ? spec.get("hp").getAsLong() : 1);
            out.put("attack", spec.has("attack") ? spec.get("attack").getAsLong() : 0);
            out.put("spec", rs.getString(7));
            out.put("loot", named(JsonParser.parseString(rs.getString(8)).getAsJsonArray()));
            return out;
         }
      }
   }

   /**
    * Lưu boss: {@code active}, {@code maps} [id], {@code respawnMinutes}, {@code firstMinutes}, {@code level} / {@code hp}
    * / {@code attack} (ghi vào spec, giữ khoá khác), {@code loot} mảng dòng quà ({@code WorldBossLoot}). Kiểm tra bằng
    * chính bộ đọc lúc nạp rồi mới ghi; nạp lại ngay (boss đang sống giữ chỉ số cũ tới lần xuất hiện sau).
    */
   private Object saveBoss(JsonObject b) throws SQLException {
      int id = intField(b, "id");
      String name;
      String specJson;
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT name, spec FROM world_boss WHERE id = ?")) {
         ps.setInt(1, id);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               throw new BadRequest("Không có boss #" + id + ".");
            }
            name = rs.getString(1);
            specJson = rs.getString(2);
         }
      }
      JsonObject spec = JsonParser.parseString(specJson).getAsJsonObject();
      if (b.has("level")) {
         spec.addProperty("level", Math.max(1, intField(b, "level")));
      }
      if (b.has("hp")) {
         spec.addProperty("hp", Math.max(1, b.get("hp").getAsLong()));
      }
      if (b.has("attack")) {
         spec.addProperty("attack", Math.max(0, b.get("attack").getAsLong()));
      }
      JsonArray maps = arr(b, "maps");
      if (maps.isEmpty()) {
         throw new BadRequest("Boss cần ít nhất 1 map.");
      }
      for (JsonElement m : maps) {
         if (d.world().mapTemplate(m.getAsInt()) == null) {
            throw new BadRequest("Không có map " + m.getAsInt() + ".");
         }
      }
      JsonArray loot = arr(b, "loot");
      for (JsonElement e : loot) {
         JsonObject o = e.getAsJsonObject();
         o.remove("name"); // tên / ảnh chỉ để hiển thị
         o.remove("icon");
         if (!o.has("i") || GameDataCache.itemTemplate(o.get("i").getAsInt()) == null) {
            throw new BadRequest("Dòng quà có mẫu item không tồn tại.");
         }
      }
      int respawn = Math.max(1, intField(b, "respawnMinutes"));
      int first = Math.max(0, intField(b, "firstMinutes"));
      String error = WorldBossService.validate(id, name, maps.toString(), respawn, first, spec.toString(),
            loot.toString());
      if (error != null) {
         throw new BadRequest(error);
      }
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("UPDATE world_boss SET active = ?, maps = ?, respawn_minutes = ?,"
                  + " first_minutes = ?, spec = ?, loot = ? WHERE id = ?")) {
         ps.setBoolean(1, bool(b, "active"));
         ps.setString(2, maps.toString());
         ps.setInt(3, respawn);
         ps.setInt(4, first);
         ps.setString(5, spec.toString());
         ps.setString(6, loot.toString());
         ps.setInt(7, id);
         ps.executeUpdate();
      }
      d.bosses().reload();
      Log.info("Admin panel: luu boss the gioi #%d (%d dong qua)", id, loot.size());
      return ok("Đã lưu boss " + name + ". Boss đang sống giữ chỉ số cũ tới lần xuất hiện sau.");
   }

   // ================================================================== giftcode (tlnt_data.giftcode)

   private Object giftcodes() throws SQLException {
      List<Map<String, Object>> list = new ArrayList<>();
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT code, name, enabled, listed, start_at, end_at, min_level,"
                  + " max_level, max_uses, uses_per_player, rewards FROM giftcode ORDER BY code");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", rs.getString(1));
            row.put("name", rs.getString(2));
            row.put("enabled", rs.getBoolean(3));
            row.put("listed", rs.getBoolean(4));
            row.put("startAt", date(rs.getTimestamp(5)));
            row.put("endAt", date(rs.getTimestamp(6)));
            row.put("minLevel", rs.getInt(7));
            row.put("maxLevel", rs.getInt(8));
            row.put("maxUses", rs.getInt(9));
            row.put("usesPerPlayer", rs.getInt(10));
            JsonArray rewards;
            try {
               rewards = named(JsonParser.parseString(rs.getString(11)).getAsJsonArray());
            } catch (RuntimeException e) {
               rewards = new JsonArray(); // JSON hỏng: trang hiện danh sách trống, lưu lại sẽ ghi đè
            }
            row.put("rewards", rewards);
            list.add(row);
         }
      }
      return list;
   }

   private Object saveGiftcode(JsonObject b) throws SQLException {
      String code = str(b, "code").trim().toUpperCase(Locale.ROOT);
      if (!code.matches("[A-Z0-9_]{3,32}")) {
         throw new BadRequest("Mã chỉ gồm chữ / số / _ (3-32 ký tự).");
      }
      JsonArray parsed;
      if (b.has("rewards") && b.get("rewards").isJsonArray()) {
         parsed = b.getAsJsonArray("rewards");
      } else {
         try {
            parsed = JsonParser.parseString(str(b, "rewards")).getAsJsonArray();
         } catch (RuntimeException e) {
            throw new BadRequest("Quà phải là danh sách item.");
         }
      }
      for (JsonElement e : parsed) {
         if (e.isJsonObject()) {
            e.getAsJsonObject().remove("name"); // tên / ảnh chỉ để hiển thị
            e.getAsJsonObject().remove("icon");
         }
      }
      if (parsed.isEmpty()) {
         throw new BadRequest("Giftcode cần ít nhất 1 món quà.");
      }
      rewards(parsed); // kiểm tra từng món
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("INSERT INTO giftcode (code, name, enabled, listed, start_at,"
                  + " end_at, min_level, max_level, class_ids, genders, player_names, max_uses, uses_per_player, rewards)"
                  + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name),"
                  + " enabled = VALUES(enabled), listed = VALUES(listed), start_at = VALUES(start_at),"
                  + " end_at = VALUES(end_at), min_level = VALUES(min_level), max_level = VALUES(max_level),"
                  + " max_uses = VALUES(max_uses), uses_per_player = VALUES(uses_per_player), rewards = VALUES(rewards)")) {
         ps.setString(1, code);
         ps.setString(2, str(b, "name"));
         ps.setBoolean(3, bool(b, "enabled"));
         ps.setBoolean(4, bool(b, "listed"));
         ps.setTimestamp(5, timestamp(str(b, "startAt")));
         ps.setTimestamp(6, timestamp(str(b, "endAt")));
         ps.setInt(7, Math.max(0, intField(b, "minLevel")));
         ps.setInt(8, Math.max(0, intField(b, "maxLevel")));
         ps.setInt(9, Math.max(0, intField(b, "maxUses")));
         ps.setInt(10, Math.max(0, intField(b, "usesPerPlayer")));
         ps.setString(11, parsed.toString());
         ps.executeUpdate();
      }
      d.giftcodes().reload();
      Log.info("Admin panel: luu giftcode %s", code);
      return ok("Đã lưu giftcode " + code + " (có hiệu lực ngay).");
   }

   private Object deleteGiftcode(String code) throws SQLException {
      try (Connection c = d.dataDb().getConnection();
            PreparedStatement ps = c.prepareStatement("DELETE FROM giftcode WHERE code = ?")) {
         ps.setString(1, code);
         if (ps.executeUpdate() == 0) {
            throw new BadRequest("Không có giftcode " + code + ".");
         }
      }
      d.giftcodes().reload();
      Log.info("Admin panel: xoa giftcode %s", code);
      return ok("Đã xoá giftcode " + code + ".");
   }

   // ================================================================== tiện ích chung (các lớp admin dùng)

   /** Mảng RewardItem JSON -> item thật (kiểm tra mẫu item tồn tại, số lượng > 0). */
   static List<ItemData> rewards(JsonArray raw) {
      List<ItemData> items = new ArrayList<>();
      long now = System.currentTimeMillis();
      if (raw == null) {
         return items;
      }
      for (JsonElement el : raw) {
         if (!el.isJsonObject()) {
            throw new BadRequest("Mỗi món phải là JSON object {\"i\":id,\"q\":số lượng}.");
         }
         JsonObject o = el.getAsJsonObject();
         RewardItem r;
         try {
            r = RewardItem.fromJson(o, false);
         } catch (RuntimeException e) {
            throw new BadRequest("Món quà sai định dạng: " + e.getMessage());
         }
         if (r == null || GameDataCache.itemTemplate(r.templateId()) == null) {
            throw new BadRequest("Không có item " + (o.has("i") ? o.get("i").getAsString() : "?") + ".");
         }
         if (r.quantity() <= 0) {
            throw new BadRequest("Số lượng phải > 0.");
         }
         items.add(r.create(now));
      }
      return items;
   }

   /** Thêm "name" (tên mẫu item, chỉ để hiển thị — lúc lưu bỏ đi) vào từng dòng quà {"i",...}. */
   private static JsonArray named(JsonArray list) {
      JsonArray out = new JsonArray();
      for (JsonElement e : list) {
         if (!e.isJsonObject()) {
            continue;
         }
         JsonObject o = e.getAsJsonObject().deepCopy();
         var t = o.has("i") ? GameDataCache.itemTemplate(o.get("i").getAsInt()) : null;
         o.addProperty("name", t == null ? "?" : t.name());
         o.addProperty("icon", AdminItems.icon(t));
         out.add(o);
      }
      return out;
   }

   static String like(String text) {
      return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
   }

   private static String date(Timestamp t) {
      return t == null ? "" : t.toLocalDateTime().format(DATE);
   }

   private static Timestamp timestamp(String text) {
      if (text == null || text.isBlank()) {
         return null;
      }
      try {
         return Timestamp.valueOf(LocalDateTime.parse(text.trim().replace('T', ' '), DATE));
      } catch (RuntimeException e) {
         throw new BadRequest("Ngày giờ dạng yyyy-MM-dd HH:mm.");
      }
   }

   static Map<String, Object> ok(String message) {
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("ok", true);
      if (message != null) {
         out.put("message", message);
      }
      return out;
   }

   private static Map<String, Object> done2(String error, String message) {
      if (error != null) {
         throw new BadRequest(error);
      }
      return ok(message);
   }

   private static String optStr(JsonObject b, String key) {
      return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsString() : null;
   }

   private static Long optLong(JsonObject b, String key) {
      try {
         return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsLong() : null;
      } catch (RuntimeException e) {
         throw new BadRequest("Trường " + key + " phải là số.");
      }
   }

   static Float optFloat(JsonObject b, String key) {
      try {
         return b.has(key) && !b.get(key).isJsonNull() && !b.get(key).getAsString().isBlank() ? b.get(key).getAsFloat()
               : null;
      } catch (RuntimeException e) {
         throw new BadRequest("Trường " + key + " phải là số.");
      }
   }

   private static Map<String, Object> done(String error) {
      if (error != null) {
         throw new BadRequest(error);
      }
      return ok("Xong.");
   }

   static String str(JsonObject b, String key) {
      return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsString() : "";
   }

   static boolean bool(JsonObject b, String key) {
      return b.has(key) && !b.get(key).isJsonNull() && b.get(key).getAsBoolean();
   }

   static JsonArray arr(JsonObject b, String key) {
      return b.has(key) && b.get(key).isJsonArray() ? b.getAsJsonArray(key) : new JsonArray();
   }

   static JsonObject obj(JsonObject b, String key) {
      if (!b.has(key) || !b.get(key).isJsonObject()) {
         throw new BadRequest("Thiếu " + key + ".");
      }
      return b.getAsJsonObject(key);
   }

   static int intField(JsonObject b, String key) {
      try {
         return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsInt() : 0;
      } catch (RuntimeException e) {
         throw new BadRequest("Trường " + key + " phải là số.");
      }
   }

   private static double num(JsonObject b, String key) {
      try {
         return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsDouble() : 0;
      } catch (RuntimeException e) {
         throw new BadRequest("Trường " + key + " phải là số.");
      }
   }

   private static int intOf(String text, String key) {
      int v = parseInt(text == null ? "" : text, Integer.MIN_VALUE);
      if (v == Integer.MIN_VALUE) {
         throw new BadRequest("Thiếu " + key + ".");
      }
      return v;
   }

   static int parseInt(String text, int fallback) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }
}
