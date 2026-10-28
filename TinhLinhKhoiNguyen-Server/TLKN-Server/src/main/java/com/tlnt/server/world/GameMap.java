package com.tlnt.server.world;

import com.tlnt.server.combat.MobRules;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.linhmoc.LinhMocRun;
import com.tlnt.server.map.MapPackets;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.map.PlantSpots;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1 map đang chạy = bố cục ({@link MapTemplate}) + các khu. Khu 0..zone_count-1 mở sẵn và luôn tồn tại; khi mọi
 * khu đều đầy thì mở thêm khu (số nhỏ nhất còn trống), khu thêm bỏ trống lâu thì tự đóng.
 *
 * Chọn khu: khu có số NHỎ NHẤT còn chỗ (vd khu 0 có 19/30, khu 1 có 30/30 -> vào khu 0).
 * Việc giữ chỗ đi qua khoá của map nên không bao giờ vượt {@code max_players_per_zone}.
 * Mỗi khu mở ra được gắn vào {@link TickEngine}; đóng khu thì gỡ ra.
 */
public final class GameMap {

   public record ZoneInfo(int id, int players, int maxPlayers, int parties) {
      public ZoneInfo(int id, int players, int maxPlayers) {
         this(id, players, maxPlayers, 0);
      }
   }

   /**
    * Map RIÊNG TƯ ({@code map_template.id}): mỗi nhân vật 1 khu riêng (instance), không ai thấy ai — 5 = Nông trại.
    * Khu riêng mở khi chủ vào lần đầu, đóng sau {@code Zone.RETIRE_AFTER_MS} trống (quay lại sớm thì vẫn là khu cũ).
    */
   public static final Set<Integer> PRIVATE_MAP_IDS = Set.of(5);
   /**
    * Chỗ trong 1 khu riêng: chủ + 1 chỗ dự phòng cho lúc đăng nhập lại (bản cũ chưa kịp rời khu thì bản mới đã giữ
    * chỗ). Chỉ {@link #reservePrivate} của CHÍNH chủ trỏ vào khu này nên người khác không bao giờ vào được.
    */
   private static final int PRIVATE_ZONE_SLOTS = 2;

   private final MapTemplate template;
   private final MapPackets packets;
   /** Quái đã nhân {@code count} (vị trí cố định), dùng chung mọi khu; HP riêng từng khu ở {@link ZoneMobs}. */
   private final List<MapTemplate.MobInstance> mobs;
   /** Chỗ cây mọc ngẫu nhiên được (mặt đất, xa cửa / NPC), dùng chung mọi khu ({@link ZonePlants}). */
   private final PlantSpots plantSpots;
   private final World world;
   private final TickEngine engine;
   /** Sắp theo số khu. Chỉ sửa dưới khoá {@code this}. */
   private final TreeMap<Integer, Zone> zones = new TreeMap<>();
   /** Giới hạn bot được bốc 1 lần cho mỗi khu; chỉ truy cập dưới khoá {@code this}. */
   private final Map<Integer, BotLimit> botLimits = new HashMap<>();
   /** Map riêng tư: id chủ -> khu riêng. Chỉ sửa dưới khoá {@code this}. */
   private final Map<Integer, Zone> privateZones = new HashMap<>();
   private final boolean privateMap;
   /** Map phó bản Tế đàn Thiên tích (34-40): khu riêng THEO LƯỢT, giữ tới hết lượt, quái không hồi sinh. */
   private final boolean altarMap;
   /** Map phó bản Rừng Linh Mộc ({@link LinhMocRules#isDungeonMap}): khu riêng THEO LƯỢT CỦA NHÓM, quái theo cấp lượt. */
   private final boolean linhMocMap;
   /** Map Lôi vực Sấm sét ({@link com.tlnt.server.thunder.ThunderRules#isThunderMap}): map công khai 1 khu, vào qua NPC. */
   private final boolean thunderMap;
   /** Rừng Linh Mộc: mã lượt -> khu của lượt ở map này. Chỉ sửa dưới khoá {@code this}. */
   private final Map<Long, Zone> runZones = new HashMap<>();
   /** Map Giải đấu Bộ lạc ({@link Arena#isArenaMap}): không có khu cố định, mỗi khu đấu ({@link Arena}) 1 khu riêng. */
   private final boolean arenaMap;
   /** Giải đấu: mã khu đấu -> khu. Chỉ sửa dưới khoá {@code this}. */
   private final Map<Long, Zone> arenaZones = new HashMap<>();
   private record BotLimit(int ceiling, int value) {
   }
   /** Chỗ trong 1 khu Rừng Linh Mộc: tổ đội tối đa 5 + 5 chỗ dự phòng cho lúc đăng nhập lại. */
   static final int LINH_MOC_ZONE_SLOTS = 10;

   GameMap(World world, MapTemplate template, MapPackets packets, TickEngine engine) {
      this.world = world;
      this.template = template;
      this.packets = packets;
      this.mobs = template.mobInstances();
      this.plantSpots = PlantSpots.of(template);
      this.engine = engine;
      this.altarMap = AltarRules.isAltarMap(template.id());
      this.linhMocMap = LinhMocRules.isDungeonMap(template.id());
      this.thunderMap = com.tlnt.server.thunder.ThunderRules.isThunderMap(template.id());
      this.privateMap = altarMap || linhMocMap || PRIVATE_MAP_IDS.contains(template.id());
      this.arenaMap = Arena.isArenaMap(template.id());
      synchronized (this) {
         for (int id = 0; !privateMap && !arenaMap && id < template.zoneCount(); id++) {
            openZone(id);
         }
      }
   }

   /** Map riêng tư: mỗi nhân vật 1 khu riêng. */
   public boolean isPrivate() {
      return privateMap;
   }

   /** Map phó bản Tế đàn Thiên tích ({@link AltarRules#isAltarMap}). */
   public boolean isAltar() {
      return altarMap;
   }

   /** Map phó bản Rừng Linh Mộc ({@link LinhMocRules#isDungeonMap}). */
   public boolean isLinhMoc() {
      return linhMocMap;
   }

   /** Map Lôi vực Sấm sét: chỉ vào qua Tư tế Buraen, không đổi khu / dịch chuyển, cờ PK ép Bộ lạc. */
   public boolean isThunder() {
      return thunderMap;
   }

   /** Map Giải đấu Bộ lạc: chỉ vào qua Tư tế Buraen (khu đấu của giải), không đổi khu / dịch chuyển, cờ PK do giải đặt. */
   public boolean isArena() {
      return arenaMap;
   }

   /**
    * Không áp luật "người cao hơn quái / boss ≥ 10 cấp chỉ gây 20% sát thương": phó bản ({@link #isDungeon}) + Lôi vực
    * Sấm sét (đã chia map theo cấp).
    */
   public boolean noLevelGapPenalty() {
      return isDungeon() || thunderMap;
   }

   /**
    * Map PHÓ BẢN: không áp luật "người cao hơn quái ≥ 10 cấp chỉ gây 20% sát thương" ({@link MobRules#playerHitsMob}).
    * = Tế đàn Thiên tích + Rừng Linh Mộc.
    */
   public boolean isDungeon() {
      return altarMap || linhMocMap;
   }

   /**
    * Giữ chỗ cho {@code playerId} khi vào map này: map riêng tư -> khu riêng của chính họ (mở nếu chưa có), map thường
    * -> khu số nhỏ nhất còn chỗ ({@link #reserveAnyZone}).
    */
   synchronized Zone reserveFor(int playerId) {
      if (altarMap || linhMocMap || arenaMap) {
         throw new IllegalStateException("Map phu ban / giai dau phai vao qua reserveAltar / reserveLinhMoc / reserveArena");
      }
      if (!privateMap) {
         return reserveAnyZone();
      }
      return reservePrivate(playerId);
   }

   /**
    * Giữ chỗ ở khu Tế đàn của lượt {@code runId} (mỗi lượt khu mới: quái đã diệt ở lượt trước không theo sang). Số khu =
    * số khu của lượt ({@code zoneNo}, giống nhau ở cả 7 map). Khu giữ nguyên (kể cả trống) tới {@code keepUntil}.
    */
   synchronized Zone reserveAltar(int ownerId, int zoneNo, long runId, long keepUntil) {
      Zone zone = privateZones.get(ownerId);
      if (zone != null && zone.runId == runId && zone.tryReserve()) {
         return zone;
      }
      if (zone != null) {
         zone.keepUntil = 0; // khu của lượt cũ: trống thì đóng ngay
         zone.closeWhenEmpty = true;
      }
      zone = new Zone(this, zoneNo, PRIVATE_ZONE_SLOTS, ownerId);
      zone.runId = runId;
      zone.keepUntil = keepUntil;
      privateZones.put(ownerId, zone);
      engine.add(zone);
      zone.tryReserve();
      Log.debug("Mo khu Te dan map %d khu %d cho #%d", template.id(), zoneNo, ownerId);
      return zone;
   }

   /** Lượt Tế đàn của {@code ownerId} kết thúc: khu của họ ở map này hết được giữ (trống 60 s thì đóng). */
   synchronized void releaseAltar(int ownerId) {
      Zone zone = privateZones.get(ownerId);
      if (zone != null) {
         zone.keepUntil = 0;
         zone.closeWhenEmpty = true; // lượt đã hết: không ai vào lại được -> trống là đóng ngay
      }
   }

   /**
    * Giữ chỗ ở khu của lượt Rừng Linh Mộc {@code run} (mở nếu chưa có: quái theo cấp lượt, không hồi sinh). Khu giữ
    * nguyên (kể cả trống) tới hết lượt. null = khu đầy.
    */
   synchronized Zone reserveLinhMoc(LinhMocRun run) {
      Zone zone = runZones.get(run.id);
      if (zone == null) {
         zone = new Zone(this, run.zoneNo, LINH_MOC_ZONE_SLOTS, run);
         runZones.put(run.id, zone);
         engine.add(zone);
         Log.debug("Mo khu Rung Linh Moc map %d khu %d (luot %d, cap %d)", template.id(), run.zoneNo, run.id,
               run.mobLevel);
      }
      zone.keepUntil = run.end;
      return zone.tryReserve() ? zone : null;
   }

   /** Lượt Rừng Linh Mộc kết thúc: khu của lượt ở map này hết được giữ (trống 60 s thì đóng). */
   synchronized void releaseLinhMoc(long runId) {
      Zone zone = runZones.get(runId);
      if (zone != null) {
         zone.keepUntil = 0;
         zone.closeWhenEmpty = true; // lượt đã hết: không ai vào lại được -> trống là đóng ngay
      }
   }

   /**
    * Giữ chỗ ở khu của khu đấu {@code arena} (mở nếu chưa có; giữ kể cả trống tới khi {@link #releaseArena}). null = khu
    * đấu đã chốt / đầy.
    */
   synchronized Zone reserveArena(Arena arena) {
      if (arena.closed) {
         return null;
      }
      Zone zone = arenaZones.get(arena.id);
      if (zone == null) {
         zone = new Zone(this, arena.zoneNo, arena.slots, arena);
         zone.keepUntil = Long.MAX_VALUE;
         arenaZones.put(arena.id, zone);
         engine.add(zone);
         Log.debug("Mo khu dau giai dau map %d khu %d (%s)", template.id(), arena.zoneNo,
               arena.qualifier ? "vong loai" : "tran");
      }
      return zone.tryReserve() ? zone : null;
   }

   /** Khu của khu đấu {@code arenaId} (null = chưa mở / đã đóng). */
   synchronized Zone arenaZone(long arenaId) {
      return arenaZones.get(arenaId);
   }

   /** Khu đấu đã chốt: không giữ khu nữa (trống là đóng ngay). */
   synchronized void releaseArena(Arena arena) {
      arena.closed = true;
      Zone zone = arenaZones.get(arena.id);
      if (zone != null) {
         zone.keepUntil = 0;
         zone.closeWhenEmpty = true;
      }
   }

   private Zone reservePrivate(int ownerId) {
      Zone zone = privateZones.get(ownerId);
      if (zone == null || !zone.tryReserve()) {
         if (zone != null) { // đầy (không nên xảy ra): thay khu mới, khu cũ tự đóng khi trống
            Log.warn("Khu rieng map %d cua #%d day, mo khu moi", template.id(), ownerId);
         }
         zone = new Zone(this, 0, PRIVATE_ZONE_SLOTS, ownerId);
         privateZones.put(ownerId, zone);
         engine.add(zone);
         zone.tryReserve();
         Log.debug("Mo khu rieng map %d cho #%d (%d khu rieng)", template.id(), ownerId, privateZones.size());
      }
      return zone;
   }

   public MapTemplate template() {
      return template;
   }

   public MapPackets packets() {
      return packets;
   }

   /** Khu cố định (0 .. zone_count-1) — nơi {@link SpawnDirector} rải cây / tổ ong. Map riêng tư: rỗng. */
   synchronized List<Zone> permanentZones() {
      List<Zone> list = new ArrayList<>();
      for (Zone zone : zones.values()) {
         if (zone.id() < template.zoneCount()) {
            list.add(zone);
         }
      }
      return list;
   }

   PlantSpots plantSpots() {
      return plantSpots;
   }

   List<MapTemplate.MobInstance> mobs() {
      return mobs;
   }

   World world() {
      return world;
   }

   /** Giữ chỗ ở khu số nhỏ nhất còn trống; mở khu mới nếu tất cả đã đầy. */
   synchronized Zone reserveAnyZone() {
      for (Zone zone : zones.values()) {
         if (zone.tryReserve()) {
            return zone;
         }
      }
      int id = 0;
      while (zones.containsKey(id)) {
         id++;
      }
      Zone zone = openZone(id);
      zone.tryReserve();
      return zone;
   }

   /** Giữ chỗ ở đúng khu {@code zoneId}; null nếu khu không tồn tại hoặc đã đầy. */
   /**
    * Giữ chỗ cho người chơi giả: khu số nhỏ nhất còn dưới trần bot riêng của khu (bốc ổn định từ 50–100% {@code maxBots})
    * và sau khi vào vẫn chừa ≥ {@code keepFree} chỗ cho người thật. Chỉ map thường. null = không còn khu hợp lệ.
    */
   synchronized Zone reserveForBot(int maxBots, int keepFree) {
      return reserveForBot(-1, maxBots, keepFree);
   }

   /** Như trên; {@code zoneId} ≥ 0 = chỉ khu đó. */
   synchronized Zone reserveForBot(int zoneId, int maxBots, int keepFree) {
      if (privateMap || arenaMap || thunderMap) {
         return null;
      }
      for (Zone zone : zones.values()) {
         if (zoneId >= 0 && zone.id() != zoneId) {
            continue;
         }
         int zoneBotLimit = botLimit(zone, maxBots);
         if (zone.botSlots.get() < zoneBotLimit && zone.occupancy() < zone.maxPlayers() - keepFree && zone.tryReserve()) {
            zone.botSlots.incrementAndGet();
            return zone;
         }
      }
      return null;
   }

   /** Bot đổi khu: khu bất kỳ (bắt đầu ngẫu nhiên) KHÁC {@code exceptZoneId} còn dưới trần bot. */
   synchronized Zone reserveForBotExcept(int exceptZoneId, int maxBots, int keepFree) {
      if (privateMap || arenaMap || thunderMap || zones.size() < 2) {
         return null;
      }
      List<Zone> list = new ArrayList<>(zones.values());
      int start = ThreadLocalRandom.current().nextInt(list.size());
      for (int i = 0; i < list.size(); i++) {
         Zone zone = list.get((start + i) % list.size());
         if (zone.id() == exceptZoneId) {
            continue;
         }
         if (zone.botSlots.get() < botLimit(zone, maxBots) && zone.occupancy() < zone.maxPlayers() - keepFree
               && zone.tryReserve()) {
            zone.botSlots.incrementAndGet();
            return zone;
         }
      }
      return null;
   }

   /**
    * Mỗi khu thường có một trần bot ổn định được bốc ngẫu nhiên từ 50% đến 100% mức cấu hình. Trần bằng sức chứa khu
    * được giữ nguyên cho bot săn boss; mức trần tính ra thay đổi thì bốc lại theo giới hạn mới.
    */
   private int botLimit(Zone zone, int ceiling) {
      if (ceiling >= zone.maxPlayers()) {
         return ceiling;
      }
      BotLimit cached = botLimits.get(zone.id());
      if (cached == null || cached.ceiling() != ceiling) {
         int minimum = Math.max(1, (ceiling + 1) / 2);
         int value = minimum >= ceiling ? ceiling : ThreadLocalRandom.current().nextInt(minimum, ceiling + 1);
         botLimits.put(zone.id(), new BotLimit(ceiling, value));
         return value;
      }
      return cached.value();
   }

   synchronized Zone reserveZone(int zoneId) {
      Zone zone = zones.get(zoneId);
      return zone != null && zone.tryReserve() ? zone : null;
   }

   synchronized int zoneCount() {
      return zones.size() + privateZones.size() + runZones.size() + arenaZones.size();
   }

   synchronized boolean hasZone(int zoneId) {
      return zones.containsKey(zoneId);
   }

   synchronized List<ZoneInfo> zoneInfos() {
      List<ZoneInfo> list = new ArrayList<>(zones.size());
      for (Zone zone : zones.values()) {
         list.add(new ZoneInfo(zone.id(), zone.occupancy(), zone.maxPlayers(), zone.partyCount()));
      }
      return list;
   }

   /** Gọi từ tick của khu phụ đã trống lâu. Kiểm tra lại dưới khoá vì có thể vừa có người giữ chỗ. */
   synchronized void retireIfEmpty(Zone zone) {
      if (zone.occupancy() > 0 || System.currentTimeMillis() < zone.keepUntil) {
         return;
      }
      if (zone.arena != null) {
         arenaZones.remove(zone.arena.id, zone);
         zone.close();
         engine.remove(zone);
         Log.debug("Dong khu dau giai dau map %d khu %d", template.id(), zone.id());
         return;
      }
      if (zone.linhMoc != null) {
         runZones.remove(zone.linhMoc.id, zone);
         zone.close();
         engine.remove(zone);
         Log.debug("Dong khu Rung Linh Moc map %d (luot %d)", template.id(), zone.linhMoc.id);
         return;
      }
      if (zone.ownerId() != 0) {
         if (privateZones.get(zone.ownerId()) == zone) {
            privateZones.remove(zone.ownerId());
         }
         zone.releaseQuotas();
         zone.close();
         engine.remove(zone);
         Log.debug("Dong khu rieng map %d cua #%d (trong)", template.id(), zone.ownerId());
         return;
      }
      if (zone.id() < template.zoneCount() || zones.get(zone.id()) != zone) {
         return;
      }
      zones.remove(zone.id());
      botLimits.remove(zone.id());
      zone.releaseQuotas(); // gọi từ luồng tick của chính khu này (Zone.checkRetire)
      zone.close();
      engine.remove(zone);
      Log.info("Dong map %d khu %d (trong)", template.id(), zone.id());
   }

   synchronized void shutdown() {
      for (Zone zone : zones.values()) {
         zone.close();
         engine.remove(zone);
      }
      for (Zone zone : privateZones.values()) {
         zone.close();
         engine.remove(zone);
      }
      for (Zone zone : runZones.values()) {
         zone.close();
         engine.remove(zone);
      }
      for (Zone zone : arenaZones.values()) {
         zone.close();
         engine.remove(zone);
      }
      arenaZones.clear();
      zones.clear();
      privateZones.clear();
      runZones.clear();
   }

   private Zone openZone(int id) {
      Zone zone = new Zone(this, id, template.maxPlayersPerZone());
      zones.put(id, zone);
      engine.add(zone);
      if (id >= template.zoneCount()) {
         Log.info("Mo them map %d khu %d (cac khu da day)", template.id(), id);
      }
      return zone;
   }
}
