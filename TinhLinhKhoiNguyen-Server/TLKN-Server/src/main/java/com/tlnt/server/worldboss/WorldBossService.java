package com.tlnt.server.worldboss;

import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Boss thế giới ({@code tlnt_data.world_boss}, {@link WorldBossDef}): cả server mỗi boss CHỈ 1 con. Bật máy chủ ->
 * {@code first_minutes} sau con đầu xuất hiện; chết -> {@code respawn_minutes} sau con mới xuất hiện ở map ngẫu nhiên
 * trong {@code maps}, khu ngẫu nhiên, chỗ đứng ngẫu nhiên xa cửa ({@code World.spawnWorldBoss}). Xuất hiện / bị hạ ->
 * chữ chạy cho mọi người đang online ({@code World.broadcastTicker}): "A vừa xuất hiện tại B khu vực C" / "A vừa tiêu
 * diệt được B! Sức mạnh thật kinh ngạc" (A = người kết liễu — người nhận quà).
 *
 * <p>Quà ({@code loot}) cho người KẾT LIỄU (last hit): rơi xuống đất cả khu thấy, chỉ người đó nhặt; không có người kết
 * liễu (chảy máu, lệnh test) -> rơi tự do. Trạng thái chỉ ở RAM (tắt máy chủ = mọi boss tính lại từ đầu).
 *
 * <p>Luồng: {@link #tick} (scheduler, mỗi giây) + {@link #reload} (pool blocking, mỗi phút — sửa bảng có hiệu lực từ lần
 * xuất hiện sau) + sự kiện từ luồng tick khu ({@code World.WorldBossListener}) — trạng thái dưới khoá {@code this}; việc
 * nặng (phát cho mọi người) đẩy sang {@code executor}.
 */
public final class WorldBossService {

   /** Không mọc được (map / khu không có): thử lại sau bấy lâu. */
   static final long RETRY_MS = 60_000;

   private final World world;
   private final WorldBossRepository repository;
   private final Executor executor;

   /** id -> cấu hình đang dùng. Dưới khoá this. */
   private final Map<Integer, WorldBossDef> defs = new LinkedHashMap<>();
   /** id -> trạng thái (giữ cả khi boss bị tắt / xoá khỏi bảng — đang sống thì vẫn sống tới chết). Dưới khoá this. */
   private final Map<Integer, State> states = new LinkedHashMap<>();

   /** Trạng thái 1 boss. */
   static final class State {
      /** Đang đứng trên map (đã báo xuất hiện, chưa chết). */
      boolean alive;
      /** Đã gửi yêu cầu mọc, chờ khu trả lời. */
      boolean spawning;
      long nextSpawnAt;
      int mapId;
      String mapName = "";
      int zoneId;
   }

   public WorldBossService(World world, WorldBossRepository repository, Executor executor) {
      this.world = world;
      this.repository = repository;
      this.executor = executor;
      instance = this;
   }

   /** Nạp lại bảng (SQL — pool blocking / lúc khởi động). Lỗi -> giữ bản cũ. */
   public void reload() {
      try {
         apply(repository.loadActive(), System.currentTimeMillis());
      } catch (SQLException e) {
         Log.warn("Khong doc duoc world_boss (giu ban cu): %s", e.getMessage());
      }
   }

   /** Thay cấu hình; boss mới (chưa có trạng thái) hẹn xuất hiện sau {@code firstMs}. */
   synchronized void apply(List<WorldBossDef> list, long now) {
      boolean first = defs.isEmpty() && states.isEmpty();
      defs.clear();
      for (WorldBossDef def : list) {
         defs.put(def.id(), def);
         State st = states.get(def.id());
         if (st == null) {
            st = new State();
            st.nextSpawnAt = now + def.firstMs();
            states.put(def.id(), st);
         }
      }
      if (first && !list.isEmpty()) {
         Log.info("Boss the gioi: %d boss, con dau tien sau %d phut", list.size(), list.get(0).firstMs() / 60_000);
      }
   }

   // ------------------------------------------------------------------ admin panel

   /**
    * 1 boss thế giới đang sống cho bot săn boss: boss quái nhắm theo {@code mobTemplateId}; boss người chơi giả
    * ({@code character}) nhắm theo mã boss {@code id} ({@code World.botChaseWorldBoss}).
    */
   public record AliveBoss(int id, String name, int mapId, int zoneId, int mobTemplateId, int level, boolean character) {
      public AliveBoss(int id, String name, int mapId, int zoneId, int mobTemplateId, int level) {
         this(id, name, mapId, zoneId, mobTemplateId, level, false);
      }
   }

   /**
    * Mọi boss thế giới đang sống bot săn được: quái (như trước) + người chơi giả (2026-10-18); boss người chơi giả không báo
    * toàn server (Lôi vực Sấm sét — chiến trường có luật riêng).
    */
   public synchronized List<AliveBoss> aliveBosses() {
      List<AliveBoss> out = new java.util.ArrayList<>();
      for (WorldBossDef def : defs.values()) {
         State st = states.get(def.id());
         if (st == null || !st.alive) {
            continue;
         }
         if (def.charSpec() != null && def.announce()) { // Tinh linh Sấm sét (Lôi vực) thì không
            out.add(new AliveBoss(def.id(), def.name(), st.mapId, st.zoneId, 0, def.level(), true));
         } else if (def.mobTemplateId() > 0) {
            out.add(new AliveBoss(def.id(), def.name(), st.mapId, st.zoneId, def.mobTemplateId(), def.level(), false));
         }
      }
      return out;
   }

   /** Boss thế giới dạng quái đang sống (boss người chơi giả không tính — bot không nhắm được). */
   public synchronized List<AliveBoss> aliveMobBosses() {
      List<AliveBoss> out = new java.util.ArrayList<>();
      for (WorldBossDef def : defs.values()) {
         State st = states.get(def.id());
         if (st != null && st.alive && def.charSpec() == null && def.mobTemplateId() > 0) {
            out.add(new AliveBoss(def.id(), def.name(), st.mapId, st.zoneId, def.mobTemplateId(), def.level()));
         }
      }
      return out;
   }

   /** Ảnh chụp 1 boss cho bảng Boss thế giới của người chơi (op 100 — {@link WorldBossList}). Bất biến. */
   public record Entry(WorldBossDef def, boolean alive, boolean spawning, int mapId, String mapName, int zoneId,
         long nextSpawnAt) {
   }

   /**
    * Boss hiện ở bảng Boss thế giới: bỏ boss không báo toàn server (Lôi vực Sấm sét — {@code "announce":false}) và boss
    * sự kiện đang tắt. Thứ tự = bảng world_boss.
    */
   public synchronized List<Entry> entries() {
      List<Entry> out = new java.util.ArrayList<>();
      for (WorldBossDef def : defs.values()) {
         State st = states.get(def.id());
         if (st != null && def.announce() && !def.eventOff()) {
            out.add(new Entry(def, st.alive, st.spawning, st.mapId, st.mapName, st.zoneId, st.nextSpawnAt));
         }
      }
      return out;
   }

   /** 1 boss của {@link #entries()}; null = không có / không hiện. */
   public Entry entry(int id) {
      for (Entry e : entries()) {
         if (e.def().id() == id) {
            return e;
         }
      }
      return null;
   }

   /** Mỗi boss: id, tên, trạng thái chữ, map / khu nếu đang sống, giây tới lần xuất hiện. */
   public synchronized List<Map<String, Object>> adminList() {
      long now = System.currentTimeMillis();
      List<Map<String, Object>> list = new java.util.ArrayList<>();
      for (WorldBossDef def : defs.values()) {
         State st = states.get(def.id());
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", def.id());
         row.put("name", def.name());
         row.put("alive", st.alive);
         row.put("mapId", st.mapId);
         row.put("zoneId", st.zoneId);
         row.put("maxHp", def.hp());
         row.put("level", def.level());
         row.put("respawnMinutes", def.respawnMs() / 60_000);
         row.put("mapName", st.alive ? st.mapName : null);
         row.put("kind", def.charSpec() != null ? "char" : "mob");
         row.put("mapCount", def.maps().size());
         row.put("spawning", st.spawning);
         row.put("nextSpawnIn", st.alive || st.spawning ? 0 : Math.max(0, (st.nextSpawnAt - now) / 1000));
         row.put("status", st.alive ? "Đang ở " + st.mapName + " khu " + st.zoneId
               : st.spawning ? "Đang xuất hiện" : "Xuất hiện sau " + Math.max(0, (st.nextSpawnAt - now) / 1000) + " s");
         list.add(row);
      }
      return list;
   }

   /**
    * Admin panel: kiểm tra 1 dòng world_boss trước khi ghi (cùng bộ đọc lúc nạp). @return câu báo lỗi, null = hợp lệ
    */
   public static String validate(int id, String name, String mapsJson, int respawnMinutes, int firstMinutes,
         String specJson, String lootJson) {
      try {
         com.google.gson.JsonArray loot = com.google.gson.JsonParser.parseString(lootJson).getAsJsonArray();
         for (int k = 0; k < loot.size(); k++) {
            if (!loot.get(k).isJsonObject() || WorldBossLoot.fromJson(loot.get(k).getAsJsonObject()) == null) {
               return "Dòng quà " + (k + 1) + " thiếu mẫu item.";
            }
         }
         WorldBossDef.parse(id, name, mapsJson, respawnMinutes, firstMinutes, specJson, lootJson, "world_boss #" + id);
         return null;
      } catch (RuntimeException e) {
         return "Cấu hình boss sai: " + e.getMessage();
      }
   }

   /** Gọi boss ra ngay (trong 1 giây). @return câu báo lỗi, null = được */
   public synchronized String adminSpawn(int id) {
      State st = states.get(id);
      if (st == null) {
         return "Không có boss " + id + ".";
      }
      if (st.alive || st.spawning) {
         return "Boss đang sống / đang xuất hiện.";
      }
      st.nextSpawnAt = System.currentTimeMillis();
      return null;
   }

   /** Hạ boss đang sống (không ai được quà kết liễu). */
   public void adminKill(int id) {
      world.testKillWorldBoss(id);
   }

   /** Mỗi giây (scheduler): boss nào tới giờ thì gửi yêu cầu mọc. */
   public void tick() {
      long now = System.currentTimeMillis();
      List<WorldBossDef> due = new java.util.ArrayList<>();
      synchronized (this) {
         for (WorldBossDef def : defs.values()) {
            State st = states.get(def.id());
            if (def.eventOff()) { // boss sự kiện đang tắt: gỡ nếu đang sống, không mọc
               if (st != null && (st.alive || st.spawning)) {
                  st.alive = false;
                  st.spawning = false;
                  st.nextSpawnAt = now + def.firstMs();
                  world.despawnWorldBoss(def.id());
               }
               continue;
            }
            if (st != null && !st.alive && !st.spawning && now >= st.nextSpawnAt) {
               st.spawning = true;
               due.add(def);
            }
         }
      }
      for (WorldBossDef def : due) {
         spawn(def);
      }
   }

   private void spawn(WorldBossDef def) {
      int mapId = def.maps().get(ThreadLocalRandom.current().nextInt(def.maps().size()));
      if (!world.hasMap(mapId)) {
         Log.warn("Boss the gioi #%d: khong co map %d", def.id(), mapId);
      }
      world.spawnWorldBoss(mapId, new World.WorldBossSpawn(def.id(), def.name(), def.mobTemplateId(), def.level(),
            def.hp(), def.attack(), def.attackMs(), def.charSpec(), def.rootMs(), def.rootTargets(),
            def.rootBurnPermille(), () -> WorldBossLoot.rollAll(def.loot(), ThreadLocalRandom.current()),
            new Listener(def), null, def.slamHitMs()));
   }

   /** Sự kiện của 1 lần xuất hiện (luồng tick khu): cập nhật trạng thái dưới khoá, việc nặng sang {@code executor}. */
   private final class Listener implements World.WorldBossListener {
      private final WorldBossDef def;

      Listener(WorldBossDef def) {
         this.def = def;
      }

      @Override
      public void spawned(int mapId, String mapName, int zoneId) {
         synchronized (WorldBossService.this) {
            State st = states.get(def.id());
            st.spawning = false;
            st.alive = true;
            st.mapId = mapId;
            st.mapName = mapName;
            st.zoneId = zoneId;
         }
         if (def.announce()) {
            String text = WorldBossDef.format(def.spawnText(), def.name(), mapName, zoneId, null);
            executor.execute(() -> world.broadcastTicker(text));
         }
      }

      @Override
      public void moved(int mapId, String mapName, int zoneId) {
         synchronized (WorldBossService.this) { // admin dời boss: chỉ cập nhật chỗ, không thông báo lại
            State st = states.get(def.id());
            st.spawning = false;
            st.alive = true;
            st.mapId = mapId;
            st.mapName = mapName;
            st.zoneId = zoneId;
         }
      }

      @Override
      public void failed() {
         synchronized (WorldBossService.this) {
            State st = states.get(def.id());
            st.spawning = false;
            st.nextSpawnAt = System.currentTimeMillis() + RETRY_MS;
         }
         Log.warn("Boss the gioi #%d: khong tim duoc khu de xuat hien, thu lai sau %d s", def.id(), RETRY_MS / 1000);
      }

      @Override
      public void killed(int killerId, String killerName) {
         String mapName;
         int zoneId;
         synchronized (WorldBossService.this) {
            State st = states.get(def.id());
            st.alive = false;
            st.nextSpawnAt = System.currentTimeMillis() + respawnMs(def.id(), def);
            mapName = st.mapName;
            zoneId = st.zoneId;
         }
         Log.info("Boss the gioi #%d bi ha (nguoi ket lieu #%d)", def.id(), killerId);
         com.tlnt.server.event.MidAutumnService.bossKilled(def.id(), killerId); // Cổ thần Dạ xoa: +25 điểm sự kiện
         if (killerId != 0 && def.announce() && !def.killText().isBlank()) {
            String text = WorldBossDef.format(def.killText(), def.name(), mapName, zoneId, killerName);
            executor.execute(() -> world.broadcastTicker(text));
         }
      }
   }

   /** Thời gian chờ con sau theo cấu hình MỚI NHẤT (sửa bảng giữa chừng có hiệu lực ngay lần chết sau). */
   private long respawnMs(int id, WorldBossDef fallback) {
      WorldBossDef def = defs.get(id);
      return (def != null ? def : fallback).respawnMs();
   }

   // ------------------------------------------------------------------ lệnh test

   private static volatile WorldBossService instance;

   private static final String USAGE = "Lệnh: /boss [info] | sinh N (xuất hiện ngay) | chet N (hạ ngay, quà như thật)"
         + " | hen N giây (con sau xuất hiện sau bấy giây)";

   /** Lệnh chat {@code /boss ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      WorldBossService s = instance;
      if (s == null) {
         session.send(Packets.notice("Chưa bật boss thế giới."));
         return;
      }
      s.test(session, args);
   }

   private void test(PlayerSession session, String[] args) {
      String sub = args.length > 1 ? args[1] : "info";
      int id = args.length > 2 ? parse(args[2], 0) : 0;
      long now = System.currentTimeMillis();
      switch (sub) {
         case "sinh" -> {
            synchronized (this) {
               State st = states.get(id);
               if (st == null || st.alive || st.spawning) {
                  session.send(Packets.notice(st == null ? "Không có boss " + id + ". " + USAGE : "Boss đang sống."));
                  return;
               }
               st.nextSpawnAt = now;
            }
            session.send(Packets.notice("Boss " + id + " sẽ xuất hiện trong 1 giây."));
         }
         case "chet" -> {
            world.testKillWorldBoss(id);
            session.send(Packets.notice("Đã hạ boss " + id + " (nếu đang sống)."));
         }
         case "hen" -> {
            long seconds = args.length > 3 ? parse(args[3], 10) : 10;
            synchronized (this) {
               State st = states.get(id);
               if (st == null || st.alive) {
                  session.send(Packets.notice(st == null ? "Không có boss " + id + "." : "Boss đang sống."));
                  return;
               }
               st.nextSpawnAt = now + seconds * 1000;
            }
            session.send(Packets.notice("Boss " + id + " xuất hiện sau " + seconds + " giây."));
         }
         default -> {
            StringBuilder sb = new StringBuilder();
            synchronized (this) {
               for (WorldBossDef def : defs.values()) {
                  State st = states.get(def.id());
                  sb.append(def.id()).append(". ").append(def.name()).append(": ");
                  if (st.alive) {
                     sb.append("đang ở ").append(st.mapName).append(" khu ").append(st.zoneId);
                  } else if (st.spawning) {
                     sb.append("đang xuất hiện");
                  } else {
                     sb.append("xuất hiện sau ").append(Math.max(0, (st.nextSpawnAt - now) / 1000)).append(" s");
                  }
                  sb.append(". ");
               }
            }
            session.send(Packets.notice(sb.length() == 0 ? "Không có boss thế giới nào. " + USAGE : sb + USAGE));
         }
      }
   }

   private static int parse(String text, int fallback) {
      try {
         return Integer.parseInt(text.trim());
      } catch (NumberFormatException e) {
         return fallback;
      }
   }
}
