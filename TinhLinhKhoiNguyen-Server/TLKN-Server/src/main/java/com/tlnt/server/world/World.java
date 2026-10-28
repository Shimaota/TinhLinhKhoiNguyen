package com.tlnt.server.world;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.altar.AltarRun;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemAppearance;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.farm.AppleTreeService;
import com.tlnt.server.gather.GatherConfig;
import com.tlnt.server.map.MapPackets;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.map.PlantSpots;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.linhmoc.LinhMocConfig;
import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.linhmoc.LinhMocRun;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.Packets;
import com.tlnt.server.party.Party;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.plant.PlantConfig;
import com.tlnt.server.plant.PlantPackets;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.PkRules;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Toàn bộ map đang chạy + người chơi đang ở trong thế giới.
 *
 * Luồng: handler (luồng worker/mạng) gọi các method public ở đây -> việc thật sự được {@link Zone#submit}
 * sang luồng tick của khu đang chứa người chơi. Chuyển khu/map = rời khu cũ trên luồng khu cũ, rồi vào khu
 * mới trên luồng khu mới (chỗ ở khu mới đã giữ trước nên không bao giờ vượt quá số người tối đa).
 */
public final class World {

   /** Khoảng cách tối đa (pixel) từ nhân vật tới cửa dịch chuyển để được dùng cửa. */
   private static final float WAYPOINT_REACH_PX = 250f;
   /** Tránh đứng ngay trong vùng cửa khi vừa tới (sẽ bị dịch chuyển ngược lại). */
   private static final float WAYPOINT_ARRIVAL_OFFSET_PX = 150f;
   private static final long TRANSFER_COOLDOWN_MS = 3_000;
   private static final long WAYPOINT_REPEAT_MS = 1_000;
   /** Khoảng cách tối thiểu giữa 2 lần bấm nút khoá nhân vật. */
   private static final long LOCK_TOGGLE_COOLDOWN_MS = 500;

   private final Map<Integer, GameMap> maps;
   private final int startMapId;
   private final TickEngine engine;
   /** playerId -> người chơi đang trong thế giới. */
   private final Map<Integer, WorldPlayer> players = new ConcurrentHashMap<>();
   /** Số khu của các lượt Tế đàn Thiên tích đang chạy. */
   private final AltarZones altarZones = new AltarZones();
   private volatile AltarHooks altarHooks;

   /**
    * Sự kiện Tế đàn Thiên tích từ luồng tick khu ({@code altar/AltarService} đăng ký lúc khởi động). Chạy trên luồng tick:
    * chỉ chuyển việc sang hàng đợi session.
    */
   public interface AltarHooks {
      /** {@code p} vừa vào 1 map, map cũ hoặc map mới là map Tế đàn. */
      void mapChanged(WorldPlayer p, int fromMapId, int toMapId);

      /** {@code p} vừa diệt hết quái map Tế đàn {@code mapId}. */
      void cleared(WorldPlayer p, int mapId);

      /** {@code p} vừa ra đòn kết liễu boss Tế đàn. */
      void bossKilled(WorldPlayer p);
   }

   public void setAltarHooks(AltarHooks hooks) {
      this.altarHooks = hooks;
   }

   AltarHooks altarHooks() {
      return altarHooks;
   }

   private volatile LinhMocHooks linhMocHooks;

   /** Phó bản Rừng Linh Mộc ({@code linhmoc/LinhMocService} đăng ký lúc khởi động). */
   public interface LinhMocHooks {
      /** Lượt đang chạy (còn vào được) có {@code playerId} trong danh sách thành viên; null = không có. Mọi luồng. */
      LinhMocRun activeRun(int playerId);

      /** Khu map {@code mapId} của lượt {@code run} vừa dọn xong. Luồng tick: chỉ chuyển việc đi. */
      void cleared(LinhMocRun run, int mapId);

      /**
       * Boss {@code bossName} của lượt vừa chết, quà đã rơi xuống đất. {@code topPlayerId} = người gây nhiều sát thương
       * nhất (được ưu tiên nhặt, 0 = không ai); {@code presentIds} = thành viên đang ở khu (nhận điểm hoạt động). Luồng
       * tick: chỉ chuyển việc đi.
       */
      void bossKilled(LinhMocRun run, int topPlayerId, String bossName, List<Integer> presentIds);
   }

   public void setLinhMocHooks(LinhMocHooks hooks) {
      this.linhMocHooks = hooks;
   }

   LinhMocHooks linhMocHooks() {
      return linhMocHooks;
   }

   private volatile ThunderHooks thunderHooks;

   /**
    * Lôi vực Sấm sét ({@code thunder/ThunderService} đăng ký lúc khởi động). Gọi trên luồng tick khu: chỉ chuyển việc sang
    * hàng đợi session.
    */
   public interface ThunderHooks {
      /** {@code p} vừa tới map Lôi vực với lượt {@code run} (thưởng điểm tham gia). */
      void entered(WorldPlayer p, com.tlnt.server.thunder.ThunderRun run);

      /** {@code p} vừa rời map Lôi vực sang map khác (về làng / bị loại / hết giờ) — lượt {@code run} kết thúc. */
      void left(WorldPlayer p, com.tlnt.server.thunder.ThunderRun run);

      /** {@code killer} vừa hạ gục {@code victim} ở Lôi vực (thưởng điểm cống hiến / hoạt động). */
      void playerKilled(WorldPlayer killer, WorldPlayer victim);
   }

   public void setThunderHooks(ThunderHooks hooks) {
      this.thunderHooks = hooks;
   }

   ThunderHooks thunderHooks() {
      return thunderHooks;
   }

   private volatile ArenaHooks arenaHooks;

   /**
    * Giải đấu Bộ lạc ({@code tournament/TournamentService} đăng ký lúc khởi động). Gọi trên luồng tick khu đấu: chỉ chuyển
    * việc đi.
    */
   public interface ArenaHooks {
      /** {@code victim} vừa bị hạ gục ở khu đấu {@code arena}; {@code killer} null = rơi đài không do ai. */
      void killed(Arena arena, WorldPlayer killer, WorldPlayer victim);

      /** {@code p} vừa rời khu của khu đấu (bị đưa về / tự thoát / bị loại / thoát game). */
      void left(Arena arena, WorldPlayer p);
   }

   public void setArenaHooks(ArenaHooks hooks) {
      this.arenaHooks = hooks;
   }

   ArenaHooks arenaHooks() {
      return arenaHooks;
   }

   // ------------------------------------------------------------------ boss thế giới

   /** Sự kiện của 1 boss thế giới ({@code worldboss/WorldBossService}). Gọi trên luồng tick khu: chỉ chuyển việc đi. */
   public interface WorldBossListener {
      /** Boss đã đứng ở map {@code mapId} ({@code mapName}) khu {@code zoneId}. */
      void spawned(int mapId, String mapName, int zoneId);

      /** Không mọc được (map không có khu / mọi khu đã có boss cùng loại). */
      void failed();

      /** Boss vừa chết, quà đã rơi xuống đất. {@code killerId} = người kết liễu (0 = chết không do đòn của ai). */
      void killed(int killerId, String killerName);

      /** Mini boss Cáo vừa rời map {@code fromMapId} (đổi map / khu): dịch vụ cho mọc lại ở map khác. */
      default void relocate(int fromMapId) {
      }

      /** Cáo rảnh ở khu (fromMapId, fromZoneId): khu / map khác có mục tiêu {mapId, zoneId}; null = ở lại. */
      default int[] seekTarget(int fromMapId, int fromZoneId) {
         return null;
      }

      /** Cáo vừa rời khu để sang ĐÚNG khu ({@code mapId}, {@code zoneId}) của mục tiêu. */
      default void relocateTo(int mapId, int zoneId) {
      }

      /** Admin vừa dời boss sang map / khu này (không thông báo lại). Mặc định = {@link #spawned}. */
      default void moved(int mapId, String mapName, int zoneId) {
         spawned(mapId, mapName, zoneId);
      }
   }

   /**
    * Yêu cầu mọc 1 boss thế giới ({@link #spawnWorldBoss}). {@code charSpec} null = boss QUÁI: mob_template
    * {@code mobTemplateId}, cấp {@code level}, {@code hp}, mỗi {@code attackMs} 1 chiêu, đòn thường {@code attack}
    * ({@link LinhMocMobBoss#world}). Có {@code charSpec} = boss người chơi giả ({@link AltarBoss} chế độ thông minh), trói
    * {@code rootMs} ms tối đa {@code rootTargets} người, rễ đốt {@code rootBurnPermille}‰ HP tối đa / giây.
    * {@code loot} tung quà lúc chết, {@code key} = mã boss (lệnh test). {@code slamHitMs} = boss quái: lúc đòn đập đất
    * (skill4) trúng trong animation, 0 = mặc định của Vệ thần Rừng xanh.
    */
   public record WorldBossSpawn(int key, String name, int mobTemplateId, int level, long hp, long attack, long attackMs,
         AltarRules.BossSpec charSpec, int rootMs, int rootTargets, int rootBurnPermille,
         java.util.function.Supplier<List<ItemData>> loot, WorldBossListener listener, FoxThief thief,
         long slamHitMs, Rabbit rabbit) {

      /** Không phải Ngọc thố. */
      public WorldBossSpawn(int key, String name, int mobTemplateId, int level, long hp, long attack, long attackMs,
            AltarRules.BossSpec charSpec, int rootMs, int rootTargets, int rootBurnPermille,
            java.util.function.Supplier<List<ItemData>> loot, WorldBossListener listener, FoxThief thief,
            long slamHitMs) {
         this(key, name, mobTemplateId, level, hp, attack, attackMs, charSpec, rootMs, rootTargets, rootBurnPermille,
               loot, listener, thief, slamHitMs, null);
      }

      /** Không chỉnh thời gian đập đất. */
      public WorldBossSpawn(int key, String name, int mobTemplateId, int level, long hp, long attack, long attackMs,
            AltarRules.BossSpec charSpec, int rootMs, int rootTargets, int rootBurnPermille,
            java.util.function.Supplier<List<ItemData>> loot, WorldBossListener listener, FoxThief thief) {
         this(key, name, mobTemplateId, level, hp, attack, attackMs, charSpec, rootMs, rootTargets, rootBurnPermille,
               loot, listener, thief, 0);
      }

      /** Boss thế giới thường (không phải cáo trộm). */
      public WorldBossSpawn(int key, String name, int mobTemplateId, int level, long hp, long attack, long attackMs,
            AltarRules.BossSpec charSpec, int rootMs, int rootTargets, int rootBurnPermille,
            java.util.function.Supplier<List<ItemData>> loot, WorldBossListener listener) {
         this(key, name, mobTemplateId, level, hp, attack, attackMs, charSpec, rootMs, rootTargets, rootBurnPermille,
               loot, listener, null);
      }
   }

   /**
    * Mọc boss thế giới ở 1 khu cố định NGẪU NHIÊN của map {@code mapId} (khu đã có boss cùng loại thì thử khu khác), chỗ
    * đứng ngẫu nhiên xa cửa ({@link WorldBossSpots}). Kết quả báo qua {@code req.listener()} (luồng tick khu). Mọi luồng.
    */
   public void spawnWorldBoss(int mapId, WorldBossSpawn req) {
      GameMap map = maps.get(mapId);
      List<Zone> zones = map == null ? new ArrayList<>() : new ArrayList<>(map.permanentZones());
      Collections.shuffle(zones, ThreadLocalRandom.current());
      trySpawnWorldBoss(zones, 0, req);
   }

   private static void trySpawnWorldBoss(List<Zone> zones, int index, WorldBossSpawn req) {
      if (index >= zones.size()) {
         req.listener().failed();
         return;
      }
      Zone zone = zones.get(index);
      zone.submit(() -> {
         if (!placeWorldBoss(zone, req)) {
            trySpawnWorldBoss(zones, index + 1, req);
         }
      });
   }

   /** Đặt boss vào khu (luồng tick khu). @return false = khu đã có boss cùng loại */
   static boolean placeWorldBoss(Zone zone, WorldBossSpawn req) {
      return placeWorldBoss(zone, req, null, -1, null);
   }

   /**
    * Như trên; {@code spot} null = chỗ ngẫu nhiên xa cửa, {@code hp} &gt; 0 = HP lúc đặt (admin dời boss giữ HP),
    * {@code carry} khác null = chép sổ sát thương cũ + báo {@code listener.moved} thay vì {@code spawned}.
    */
   static boolean placeWorldBoss(Zone zone, WorldBossSpawn req, float[] spot, long hp, DamageLedger carry) {
      boolean isChar = req.charSpec() != null;
      if (isChar ? zone.boss != null && !zone.boss.dead : zone.mobBoss != null) {
         return false;
      }
      MapTemplate map = zone.map().template();
      if (spot == null) {
         spot = WorldBossSpots.pick(map, ThreadLocalRandom.current());
      }
      WorldBoss wb;
      if (isChar) {
         AltarBoss boss = new AltarBoss(req.charSpec(), AltarBoss.worldId(WORLD_BOSS_SEQ.incrementAndGet()), spot[0],
               spot[1]);
         if (hp > 0) {
            boss.hp = Math.min(hp, req.charSpec().hp());
         }
         if (req.thief() != null) { // mini boss Cáo: AI trộm, giữ vàng đã trộm qua các lần đổi map
            boss.thief = req.thief();
            boss.thief.moved();
         } else if (req.rabbit() != null) { // Ngọc thố Trung thu: chạy khắp map, ăn cà rốt, không đánh / không chết
            boss.rabbit = req.rabbit();
         } else {
            boss.smart = true;
            boss.pierceArmor = true;
         }
         boss.rootMs = req.rootMs() > 0 ? req.rootMs() : boss.rootMs;
         boss.rootTargets = req.rootTargets() > 0 ? req.rootTargets() : boss.rootTargets;
         boss.rootBurnPermille = Math.max(0, req.rootBurnPermille());
         zone.boss = boss;
         if (carry != null) {
            boss.damageLedger.copyFrom(carry);
         }
         zone.broadcast(WorldPackets.bossSpawn(boss), null);
         wb = new WorldBoss(req, boss);
      } else {
         int id = zone.mobs().addManual(MapTemplate.MobSpawn.of(req.mobTemplateId(), spot[0], spot[1], req.level(),
               req.hp()), spot[0], spot[1], zone.broadcastAll());
         if (hp > 0 && hp < req.hp()) {
            zone.mobs().damage(id, req.hp() - hp, System.currentTimeMillis());
            zone.broadcast(WorldPackets.mobDamaged(id, 0, hp).toPacket(), null);
         }
         LinhMocMobBoss ai = LinhMocMobBoss.world(id, req.attackMs(), req.attack(), req.slamHitMs());
         if (carry != null) {
            ai.damage.copyFrom(carry);
         }
         zone.mobBoss = ai;
         wb = new WorldBoss(req, id, ai);
      }
      zone.worldBosses.add(wb);
      Log.info("Boss the gioi #%d %s: map %d khu %d (%.0f, %.0f)", req.key(), carry != null ? "duoc doi toi"
            : "xuat hien", map.id(), zone.id(), spot[0], spot[1]);
      if (carry != null) {
         req.listener().moved(map.id(), map.name(), zone.id());
      } else {
         req.listener().spawned(map.id(), map.name(), zone.id());
      }
      return true;
   }

   /**
    * Cáo trộm rời khu (đổi map / khu): gỡ khỏi khu (op 4 cả khu), bỏ khỏi danh sách boss thế giới của khu (không phát
    * quà), báo dịch vụ mọc lại ở map khác với CÙNG trạng thái (vàng đã trộm), đầy HP. Luồng tick khu.
    */
   static void foxLeave(Zone zone, AltarBoss boss) {
      WorldBoss found = removeFox(zone, boss);
      if (found != null) {
         found.request.listener().relocate(zone.map().template().id());
      }
   }

   /** Gỡ cáo khỏi khu (op 4 cả khu, bỏ khỏi danh sách boss thế giới, không phát quà). @return mục boss thế giới */
   private static WorldBoss removeFox(Zone zone, AltarBoss boss) {
      WorldBoss found = null;
      for (WorldBoss wb : zone.worldBosses) {
         if (wb.charBoss == boss) {
            found = wb;
         }
      }
      if (found != null) {
         zone.worldBosses.remove(found);
      }
      if (zone.boss == boss) {
         zone.boss = null;
      }
      boss.gone = true;
      boss.clearCarrots(zone);
      zone.broadcast(WorldPackets.playerLeave(boss.id), null);
      if (boss.thief != null) {
         Log.debug("Cao #%d roi map %d khu %d (da trom %d)", boss.thief.slot, zone.map().template().id(), zone.id(),
               boss.thief.total());
      }
      return found;
   }

   /** Ngọc thố ăn đủ cà rốt: gỡ khỏi khu (không quà), dịch vụ thỏ cho mọc ở map khác. Luồng tick khu. */
   static void rabbitLeave(Zone zone, AltarBoss boss) {
      WorldBoss found = removeFox(zone, boss);
      if (found != null) {
         found.request.listener().relocate(zone.map().template().id());
      }
   }

   /**
    * Gỡ IM LẶNG boss thế giới người chơi giả mã {@code key} (không quà, không báo chết) — tắt sự kiện. Mọi luồng.
    */
   public void despawnWorldBoss(int key) {
      for (GameMap map : maps.values()) {
         for (Zone zone : map.permanentZones()) {
            zone.submit(() -> {
               for (WorldBoss wb : new ArrayList<>(zone.worldBosses)) {
                  if (wb.request.key() == key && wb.charBoss != null && !wb.charBoss.gone) {
                     removeFox(zone, wb.charBoss);
                  }
               }
            });
         }
      }
   }

   /**
    * Ném cà rốt cho Ngọc thố gần nhất trong khu (trong {@link Rabbit#FEED_RANGE_X} x {@link Rabbit#FEED_RANGE_Y}): củ
    * rơi ra trước mặt người ném, hiện bằng vật trang trí op 81. {@code thrown} (ném được) / {@code none} (không có thỏ ở gần) chạy trên LUỒNG TICK KHU —
    * người gọi tự đẩy về hàng đợi session. Mọi luồng.
    */
   public void feedRabbit(PlayerSession session, Runnable thrown, Runnable none) {
      onPlayerZone(session, (zone, p) -> {
         long now = System.currentTimeMillis();
         AltarBoss best = null;
         float bestDx = Float.MAX_VALUE;
         for (WorldBoss wb : zone.worldBosses) {
            AltarBoss b = wb.charBoss;
            if (b == null || b.rabbit == null || b.gone || !b.rabbit.accepting()) {
               continue;
            }
            float dx = Math.abs(b.x - p.x);
            if (dx <= Rabbit.FEED_RANGE_X && Math.abs(b.y - p.y) <= Rabbit.FEED_RANGE_Y && dx < bestDx) {
               best = b;
               bestDx = dx;
            }
         }
         if (best == null) {
            none.run();
            return;
         }
         MapTemplate map = zone.map().template();
         float cx = map.clampX(p.facingLeft ? p.x - Rabbit.THROW_AHEAD_PX : p.x + Rabbit.THROW_AHEAD_PX);
         Rabbit.Carrot c = best.rabbit.addCarrot(cx, PlantSpots.groundBelow(map, cx, p.y), p.id(), now);
         // hiện củ trên mặt đất bằng vật trang trí op 81 (client MapPropEntity)
         zone.broadcast(WorldPackets.mapPropAdd(c.id(), Rabbit.carrotTexture(), c.x(), c.y()), null);
         thrown.run();
      });
   }

   /**
    * Thả đèn hoa đăng: người chơi phải đứng gần mặt nước ({@code reachPx}); mỗi vùng nước của khu tối đa {@code max} đèn
    * cùng lúc. Đèn = vật thể trôi op 80 (như game gốc — decompile C3524, mẫu {@code map_effect} 1 "Hoa đăng"): thả ở chỗ
    * người chơi trên mặt nước, trôi sang phải {@code speedPx} px / s tới mép phải vùng nước rồi mất (client tự xoá; server
    * chỉ tính giờ mất để đếm số đèn). Được -> chạy {@code released}; không được -> {@code failed} nhận câu báo. Callback
    * chạy trên LUỒNG TICK KHU.
    */
   /** Mẫu {@code tlnt_data.map_effect} của đèn hoa đăng. */
   static final int LANTERN_EFFECT = 1;
   static final float LANTERN_SINK_PX = 50f;
   static final float LANTERN_MIN_TRAVEL_PX = 300f;
   private static final java.util.concurrent.atomic.AtomicInteger LANTERN_IDS = new java.util.concurrent.atomic.AtomicInteger();

   public void releaseLantern(PlayerSession session, float reachPx, int max, float speedPx, Runnable released,
         java.util.function.Consumer<String> failed) {
      onPlayerZone(session, (zone, p) -> {
         MapTemplate map = zone.map().template();
         List<MapTemplate.Water> waters = map.waters();
         int index = -1;
         for (int i = 0; i < waters.size(); i++) {
            if (waters.get(i).near(p.x, p.y, reachPx)) {
               index = i;
               break;
            }
         }
         if (index < 0) {
            failed.accept("Hãy đứng gần sông, hồ để thả đèn hoa đăng.");
            return;
         }
         long now = System.currentTimeMillis();
         java.util.ArrayDeque<Long> active = zone.lanterns.computeIfAbsent(index, k -> new java.util.ArrayDeque<>());
         while (!active.isEmpty() && active.peekFirst() <= now) {
            active.pollFirst();
         }
         if (active.size() >= max) {
            failed.accept("Mặt nước đã có " + max + " chiếc đèn, hãy đợi chiếc đèn đầu tiên trôi đi hết.");
            return;
         }
         MapTemplate.Water w = waters.get(index);
         float endX = w.x() + w.w();
         // thả lùi vào trong 1 đoạn nếu đứng sát mép phải (đèn vừa thả đã quá mốc thì client xoá ngay)
         float x = Math.max(w.x(), Math.min(endX - LANTERN_MIN_TRAVEL_PX, p.x));
         // Giữ nguyên vị trí hai hàng hoa đăng trong mặt nước; phần đèn bị nước che là do render layer phía client.
         float y = w.y() + w.h() - LANTERN_SINK_PX
               - (ThreadLocalRandom.current().nextBoolean() ? LANTERN_SINK_PX : 0);
         long lifeMs = (long) (Math.max(0, endX - x) / Math.max(1f, speedPx) * 1000);
         active.addLast(now + lifeMs);
         int id = -1 - (LANTERN_IDS.getAndIncrement() & 0x3FFFFFFF);
         zone.broadcast(WorldPackets.floatAway(id, LANTERN_EFFECT, x, y, endX, speedPx / 100f), null);
         released.run();
      });
   }

   /**
    * Dịch vụ cáo hỏi thăm con cáo mã {@code key} đang ở khu ({@code mapId}, {@code zoneId}) — chạy trên luồng tick khu cả
    * khi khu không có ai (lệnh xếp hàng làm khu tick). Mọi luồng.
    */
   public void pokeFox(int mapId, int zoneId, int key) {
      GameMap map = maps.get(mapId);
      if (map == null) {
         return;
      }
      for (Zone zone : map.permanentZones()) {
         if (zone.id() == zoneId) {
            zone.submit(() -> foxCheck(zone, key));
         }
      }
   }

   /**
    * Cáo rảnh (không có mục tiêu trong khu, chưa bị phát hiện, chưa đủ 1 triệu) quá {@link FoxThief#SEEK_AFTER_MS} -> hỏi
    * dịch vụ 1 khu / map khác có người đứng im còn vàng khoá; có thì rời khu này sang đó (đầy HP). Luồng tick khu.
    */
   static void foxCheck(Zone zone, int key) {
      WorldBoss wb = null;
      for (WorldBoss w : zone.worldBosses) {
         if (w.request.key() == key && w.charBoss != null && w.charBoss.thief != null && !w.charBoss.dead) {
            wb = w;
         }
      }
      if (wb == null) {
         return;
      }
      FoxThief t = wb.charBoss.thief;
      long now = System.currentTimeMillis();
      boolean idle = t.detectedAt == 0 && !t.full() && (t.targetId == 0 || zone.find(t.targetId) == null);
      if (!idle) {
         t.idleSince = 0;
         return;
      }
      if (t.idleSince == 0) {
         t.idleSince = now;
         return;
      }
      if (now - t.idleSince < FoxThief.SEEK_AFTER_MS) {
         return;
      }
      int[] dest = wb.request.listener().seekTarget(zone.map().template().id(), zone.id());
      if (dest == null) {
         return;
      }
      removeFox(zone, wb.charBoss);
      wb.request.listener().relocateTo(dest[0], dest[1]);
   }

   /**
    * Người chơi đang đứng quanh 1 chỗ ≥ {@link FoxThief#STILL_MS} ở map trong {@code maps} (khu thường), còn vàng khoá,
    * không khoá nhân vật — {mapId, zoneId, playerId}. Gọi định kỳ (luồng scheduler): tự cập nhật mốc đứng của từng người
    * (đọc vị trí không khoá — lệch 1 nhịp không sao).
    */
   public List<int[]> foxCandidates(java.util.Set<Integer> mapIds, long now) {
      List<int[]> out = new ArrayList<>();
      for (WorldPlayer p : players.values()) {
         Zone zone = p.zone;
         if (zone == null || p.removed) {
            continue;
         }
         int mapId = zone.map().template().id();
         float px = p.x;
         float py = p.y;
         if (Float.isNaN(p.stillX) || p.stillZone != zone || Math.abs(px - p.stillX) > FoxThief.STILL_RADIUS_PX
               || Math.abs(py - p.stillY) > FoxThief.STILL_RADIUS_PX) {
            p.stillX = px;
            p.stillY = py;
            p.stillZone = zone;
            p.stillSince = now;
            continue;
         }
         PlayerData d = p.data();
         if (now - p.stillSince >= FoxThief.STILL_MS && mapIds.contains(mapId) && !p.locked && !d.isDead()
               && d.lockedGold > 0) {
            out.add(new int[] {mapId, zone.id(), p.id()});
         }
      }
      return out;
   }

   /** Mọc boss thế giới vào ĐÚNG khu ({@code mapId}, {@code zoneId}); khu không có / đã có boss -> listener.failed. */
   public void spawnWorldBossInZone(int mapId, int zoneId, WorldBossSpawn req) {
      GameMap map = maps.get(mapId);
      Zone found = null;
      if (map != null) {
         for (Zone zone : map.permanentZones()) {
            if (zone.id() == zoneId) {
               found = zone;
            }
         }
      }
      if (found == null) {
         req.listener().failed();
         return;
      }
      Zone zone = found;
      zone.submit(() -> {
         if (!placeWorldBoss(zone, req)) {
            req.listener().failed();
         }
      });
   }

   private static final java.util.concurrent.atomic.AtomicInteger WORLD_BOSS_SEQ =
         new java.util.concurrent.atomic.AtomicInteger();

   /**
    * Admin panel: HP boss thế giới mã {@code key} đang ở khu ({@code mapId}, {@code zoneId}) — {hp còn, hp tối đa}; null
    * = không thấy / đã chết. Đọc trên luồng tick khu (future xong ở đó). Mọi luồng.
    */
   /** Toạ độ boss thế giới mã {@code key} ở map / khu ({x, y}); null = không thấy / đã chết. Future xong trên luồng tick. */
   public java.util.concurrent.CompletableFuture<float[]> worldBossSpot(int mapId, int zoneId, int key) {
      java.util.concurrent.CompletableFuture<float[]> out = new java.util.concurrent.CompletableFuture<>();
      GameMap map = maps.get(mapId);
      Zone found = null;
      if (map != null) {
         for (Zone zone : map.permanentZones()) {
            if (zone.id() == zoneId) {
               found = zone;
            }
         }
      }
      if (found == null) {
         out.complete(null);
         return out;
      }
      Zone zone = found;
      zone.submit(() -> {
         float[] spot = null;
         for (WorldBoss wb : zone.worldBosses) {
            if (wb.request.key() == key && !wb.dead(zone)) {
               MapTemplate.MobInstance mob = wb.mobId >= 0 ? zone.mobs().alive(wb.mobId) : null;
               if (mob != null) {
                  spot = new float[] {mob.x(), mob.y()};
               } else if (wb.charBoss != null) {
                  spot = new float[] {wb.charBoss.x, wb.charBoss.y};
               }
            }
         }
         out.complete(spot);
      });
      return out;
   }

   public java.util.concurrent.CompletableFuture<long[]> worldBossHp(int mapId, int zoneId, int key) {
      java.util.concurrent.CompletableFuture<long[]> out = new java.util.concurrent.CompletableFuture<>();
      GameMap map = maps.get(mapId);
      Zone found = null;
      if (map != null) {
         for (Zone zone : map.permanentZones()) {
            if (zone.id() == zoneId) {
               found = zone;
            }
         }
      }
      if (found == null) {
         out.complete(null);
         return out;
      }
      Zone zone = found;
      zone.submit(() -> {
         long[] hp = null;
         for (WorldBoss wb : zone.worldBosses) {
            if (wb.request.key() == key && !wb.dead(zone)) {
               hp = wb.charBoss != null ? new long[] {wb.charBoss.hp, wb.charBoss.spec.hp()}
                     : new long[] {zone.mobs().hp(wb.mobId), wb.request.hp()};
            }
         }
         out.complete(hp);
      });
      return out;
   }

   /**
    * Admin: bảng sát thương lên boss thế giới mã {@code key} đang sống ở map / khu: mỗi dòng {id, tên, sát thương, còn ở
    * khu}; null = boss không còn. Nếu {@code editPlayer} > 0 thì đặt sát thương người đó = {@code editDamage} (≤ 0 = xoá
    * khỏi sổ) TRƯỚC khi đọc. Chạy trên luồng tick khu. Mọi luồng.
    */
   public java.util.concurrent.CompletableFuture<List<Object[]>> worldBossDamage(int mapId, int zoneId, int key,
         int editPlayer, long editDamage) {
      java.util.concurrent.CompletableFuture<List<Object[]>> out = new java.util.concurrent.CompletableFuture<>();
      GameMap map = maps.get(mapId);
      Zone found = null;
      if (map != null) {
         for (Zone zone : map.permanentZones()) {
            if (zone.id() == zoneId) {
               found = zone;
            }
         }
      }
      if (found == null) {
         out.complete(null);
         return out;
      }
      Zone zone = found;
      zone.submit(() -> {
         List<Object[]> rows = null;
         for (WorldBoss wb : zone.worldBosses) {
            if (wb.request.key() == key && !wb.dead(zone)) {
               DamageLedger ledger = wb.ledger();
               if (editPlayer > 0) {
                  ledger.set(editPlayer, editDamage);
               }
               rows = new ArrayList<>();
               for (long[] r : ledger.ranking()) {
                  int id = (int) r[0];
                  WorldPlayer p = zone.find(id);
                  String name = p != null ? p.data().name : ledger.name(id);
                  rows.add(new Object[] {id, name, r[1], p != null});
               }
            }
         }
         out.complete(rows);
      });
      return out;
   }

   /** Khu cố định {@code zoneId} của map {@code mapId}; null = không có. */
   Zone permanentZone(int mapId, int zoneId) {
      GameMap map = maps.get(mapId);
      if (map != null) {
         for (Zone zone : map.permanentZones()) {
            if (zone.id() == zoneId) {
               return zone;
            }
         }
      }
      return null;
   }

   /**
    * Admin dời boss thế giới / cáo mã {@code key} từ khu (fromMap, fromZone) sang khu (toMap, toZone), chỗ ({@code x},
    * {@code y}) — null = ngẫu nhiên xa cửa; y tự hạ xuống mặt đất. Giữ HP + sổ sát thương (cáo giữ vàng đã trộm). Khu đích
    * đã có boss cùng loại -> trả boss về khu cũ. Không phát quà, không thông báo. Mọi luồng. @return câu lỗi, null = được
    */
   public java.util.concurrent.CompletableFuture<String> adminMoveWorldBoss(int key, int fromMap, int fromZone,
         int toMap, int toZone, Float x, Float y) {
      java.util.concurrent.CompletableFuture<String> out = new java.util.concurrent.CompletableFuture<>();
      Zone src = permanentZone(fromMap, fromZone);
      Zone dst = permanentZone(toMap, toZone);
      if (src == null) {
         out.complete("Không thấy khu của boss.");
         return out;
      }
      if (dst == null) {
         out.complete("Map " + toMap + " không có khu " + toZone + " (chỉ khu thường).");
         return out;
      }
      MapTemplate dstMap = dst.map().template();
      float[] spot = x == null ? null
            : new float[] {x, PlantSpots.groundBelow(dstMap, x, y == null ? 100_000f : y)};
      src.submit(() -> {
         WorldBoss wb = null;
         for (WorldBoss w : src.worldBosses) {
            if (w.request.key() == key && !w.dead(src)) {
               wb = w;
            }
         }
         if (wb == null) {
            out.complete("Boss không còn ở khu này.");
            return;
         }
         long hp = wb.charBoss != null ? wb.charBoss.hp : src.mobs().hp(wb.mobId);
         DamageLedger carry = new DamageLedger();
         carry.copyFrom(wb.ledger());
         removeWorldBoss(src, wb);
         WorldBoss moved = wb;
         dst.submit(() -> {
            if (placeWorldBoss(dst, moved.request, spot, hp, carry)) {
               out.complete(null);
               return;
            }
            src.submit(() -> { // khu đích đã có boss cùng loại: trả về khu cũ
               if (!placeWorldBoss(src, moved.request, null, hp, carry)) {
                  moved.request.listener().failed();
               }
            });
            out.complete("Khu đích đã có boss cùng loại — boss được trả về khu cũ.");
         });
      });
      return out;
   }

   /** Gỡ boss thế giới khỏi khu, KHÔNG phát quà / báo chết (admin dời đi). Luồng tick khu. */
   private static void removeWorldBoss(Zone zone, WorldBoss wb) {
      zone.worldBosses.remove(wb);
      if (wb.charBoss != null) {
         if (zone.boss == wb.charBoss) {
            zone.boss = null;
         }
         wb.charBoss.gone = true;
         zone.broadcast(WorldPackets.playerLeave(wb.charBoss.id), null);
      } else {
         if (zone.mobBoss == wb.mobAi) {
            zone.mobBoss = null;
         }
         long hp = zone.mobs().hp(wb.mobId);
         if (hp > 0) {
            zone.mobs().damage(wb.mobId, hp, System.currentTimeMillis()); // chỗ mọc tay: không hồi sinh
         }
         zone.broadcast(WorldPackets.mobDamaged(wb.mobId, 0, 0).toPacket(), null);
      }
   }

   /**
    * Admin: trạng thái con cáo trộm mã {@code key} ở khu: hp, maxHp, x, y, total (vàng đã trộm), target, detected (đang bị
    * truy đuổi), full, victims [[id, tên, số vàng]]; {@code refund} = xoá sổ trộm (vàng trả qua thư do admin gửi), danh
    * sách trả về là bản TRƯỚC khi xoá. null = không thấy. Đọc trên luồng tick khu. Mọi luồng.
    */
   public java.util.concurrent.CompletableFuture<Map<String, Object>> adminFoxState(int mapId, int zoneId, int key,
         boolean refund) {
      java.util.concurrent.CompletableFuture<Map<String, Object>> out = new java.util.concurrent.CompletableFuture<>();
      Zone zone = permanentZone(mapId, zoneId);
      if (zone == null) {
         out.complete(null);
         return out;
      }
      zone.submit(() -> {
         Map<String, Object> m = null;
         for (WorldBoss wb : zone.worldBosses) {
            if (wb.request.key() != key || wb.charBoss == null || wb.charBoss.thief == null || wb.charBoss.dead) {
               continue;
            }
            AltarBoss b = wb.charBoss;
            FoxThief t = b.thief;
            m = new LinkedHashMap<>();
            m.put("hp", b.hp);
            m.put("maxHp", b.spec.hp());
            m.put("x", Math.round(b.x));
            m.put("y", Math.round(b.y));
            m.put("total", t.total);
            m.put("full", t.full());
            m.put("detected", t.detectedAt > 0);
            m.put("target", t.targetId);
            WorldPlayer target = t.targetId == 0 ? null : zone.find(t.targetId);
            m.put("targetName", target == null ? "" : target.data().name);
            List<Object[]> victims = new ArrayList<>();
            t.stolenFrom.forEach((id, amount) -> {
               WorldPlayer p = zone.find(id);
               victims.add(new Object[] {id, p == null ? "" : p.data().name, amount});
            });
            victims.sort((a, c) -> Long.compare((Long) c[2], (Long) a[2]));
            m.put("victims", victims);
            if (refund) {
               t.total = 0;
               t.stolenFrom.clear();
            }
         }
         out.complete(m);
      });
      return out;
   }

   /**
    * Tắt sự kiện: mọi cây mẫu {@code plantTemplateId} đang mọc (vd Cà rốt Trung thu) biến mất như admin xoá. Cấu hình cây
    * đã bỏ loại này ({@code PlantRepository} lọc "event") nên không mọc lại. Mọi luồng.
    */
   public void removePlants(int plantTemplateId) {
      for (GameMap map : maps.values()) {
         for (Zone zone : map.permanentZones()) {
            zone.submit(() -> {
               long now = System.currentTimeMillis();
               List<Integer> ids = new ArrayList<>();
               for (ZonePlants.Plant plant : zone.plants().alive()) {
                  if (plant.templateId == plantTemplateId) {
                     ids.add(plant.id);
                  }
               }
               for (int id : ids) {
                  zone.plants().adminRemove(id, now, zone.broadcastAll());
               }
            });
         }
      }
   }

   /** Bật sự kiện: gửi NPC mẫu {@code npcTemplateId} của map {@code mapId} cho người đang ở map đó (op 24 sub 0). */
   public void showNpc(int mapId, int npcTemplateId) {
      GameMap map = maps.get(mapId);
      if (map == null) {
         return;
      }
      Packet packet = com.tlnt.server.map.MapPackets.npcPacket(map.template(), npcTemplateId);
      if (packet == null) {
         return;
      }
      for (Zone zone : map.permanentZones()) {
         zone.submit(() -> zone.broadcast(packet, null));
      }
   }

   /**
    * Admin xoá 1 tài nguyên ({@code kind} plant = cây / còn lại = quái săn bắt) id thực thể {@code entityId} ở khu: cây đặt
    * tay mọc lại đúng chỗ sau respawn, cây / quái ngẫu nhiên giữ suất rồi mọc chỗ khác, cái admin đặt thì hết. Mọi luồng.
    * @return câu lỗi, null = được
    */
   public java.util.concurrent.CompletableFuture<String> adminRemoveResource(String kind, int mapId, int zoneId,
         int entityId) {
      java.util.concurrent.CompletableFuture<String> out = new java.util.concurrent.CompletableFuture<>();
      Zone zone = permanentZone(mapId, zoneId);
      if (zone == null) {
         out.complete("Không có khu " + zoneId + " ở map " + mapId + ".");
         return out;
      }
      zone.submit(() -> {
         long now = System.currentTimeMillis();
         boolean ok = "plant".equals(kind) ? zone.plants().adminRemove(entityId, now, zone.broadcastAll())
               : zone.mobs().adminRemoveGather(entityId, now, zone.broadcastAll());
         out.complete(ok ? null : "Tài nguyên không còn (đã bị hái / phá).");
      });
      return out;
   }

   /**
    * Admin đặt thêm {@code count} tài nguyên ({@code kind} plant: plant_template {@code templateId}; khác: quái săn bắt
    * mob_template {@code templateId}) ở khu {@code zoneId} (&lt; 0 = MỌI khu thường của map), chỗ ({@code x}, {@code y}) — null =
    * ngẫu nhiên (cây: mặt đất; quái: điểm mọc cấu hình); nhiều cái cùng chỗ thì xếp ngang cách 120 px, y hạ xuống mặt đất.
    * Ngoài suất toàn server, không mọc lại. Mọi luồng. @return số đã đặt được
    */
   public java.util.concurrent.CompletableFuture<Integer> adminAddResource(String kind, int templateId, int mapId,
         int zoneId, Float x, Float y, int count) {
      java.util.concurrent.CompletableFuture<Integer> out = new java.util.concurrent.CompletableFuture<>();
      GameMap map = maps.get(mapId);
      List<Zone> zones = new ArrayList<>();
      if (map != null) {
         for (Zone z : map.permanentZones()) {
            if (zoneId < 0 || z.id() == zoneId) {
               zones.add(z);
            }
         }
      }
      if (zones.isEmpty()) {
         out.complete(0);
         return out;
      }
      GatherConfig.Entry gather = "plant".equals(kind) ? null : GatherConfig.current().entry(templateId);
      java.util.concurrent.atomic.AtomicInteger placed = new java.util.concurrent.atomic.AtomicInteger();
      java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(zones.size());
      for (Zone zone : zones) {
         zone.submit(() -> {
            try {
               for (int k = 0; k < count; k++) {
                  Float px = x == null ? null : x + k * 120f;
                  Float py = px == null ? null : PlantSpots.groundBelow(map.template(), px, y == null ? 100_000f : y);
                  int id = gather == null ? zone.plants().adminSpawn(templateId, px, py, zone.broadcastAll())
                        : zone.mobs().adminSpawnGather(gather, px, py, zone.broadcastAll());
                  if (id >= 0) {
                     placed.incrementAndGet();
                  }
               }
            } finally {
               if (left.decrementAndGet() == 0) {
                  out.complete(placed.get());
               }
            }
         });
      }
      return out;
   }

   /** Lệnh test: hạ ngay boss thế giới mã {@code key} (ở bất kỳ map / khu nào); quà như boss chết thật. Mọi luồng. */
   public void testKillWorldBoss(int key) {
      for (GameMap map : maps.values()) {
         for (Zone zone : map.permanentZones()) {
            zone.submit(() -> {
               for (WorldBoss wb : new ArrayList<>(zone.worldBosses)) {
                  if (wb.request.key() != key || wb.dead(zone)) {
                     continue;
                  }
                  if (wb.charBoss != null) {
                     long hp = wb.charBoss.hp;
                     wb.charBoss.damage(zone, hp);
                     zone.broadcast(WorldPackets.playerDamaged(wb.charBoss.id, hp, 0), null);
                  } else {
                     long hp = zone.mobs().hp(wb.mobId);
                     zone.mobs().damage(wb.mobId, hp, System.currentTimeMillis());
                     zone.broadcast(WorldPackets.mobDamaged(wb.mobId, hp, 0).toPacket(), null);
                  }
               }
               ZoneCombat.worldBossCheck(zone);
            });
         }
      }
   }

   /** Chữ chạy + dòng "Hệ thống" trong chat cho MỌI người đang online (op 14 sub 1), mã hoá 1 lần. Mọi luồng. */
   public void broadcastTicker(String text) {
      broadcastAll(Packets.ticker(text).toPacket());
   }

   /** Gửi 1 gói đã mã hoá cho MỌI người đang online (chat thế giới...). Mọi luồng. */
   public void broadcastAll(Packet packet) {
      for (WorldPlayer p : players.values()) {
         if (!p.removed) {
            p.send(packet);
         }
      }
   }

   public World(List<MapTemplate> templates, int startMapId, int tickThreads, long tickMillis) {
      this.engine = new TickEngine(tickThreads, tickMillis);

      Set<Integer> mapIds = new HashSet<>();
      for (MapTemplate template : templates) {
         mapIds.add(template.id());
      }
      Map<Integer, GameMap> built = new HashMap<>();
      for (MapTemplate raw : templates) {
         // Cửa dẫn tới map chưa có: không gửi cho client (tránh chạm vào là báo lỗi liên tục).
         List<MapTemplate.Waypoint> usable = new ArrayList<>();
         for (MapTemplate.Waypoint waypoint : raw.waypoints()) {
            if (waypoint.targetMapId() != null && mapIds.contains(waypoint.targetMapId())) {
               usable.add(waypoint);
            } else {
               Log.info("Map %d: an cua '%s' (map dich chua co)", raw.id(), Log.ascii(waypoint.name()));
            }
         }
         MapTemplate template = raw.withContent(usable, usableNpcs(raw), usableMobs(raw), usablePlants(raw));
         built.put(template.id(), new GameMap(this, template, new MapPackets(template), engine));
      }
      this.maps = Collections.unmodifiableMap(built);

      if (!maps.containsKey(startMapId)) {
         throw new IllegalStateException("Map xuat hien dau tien (player.startMapId=" + startMapId
               + ") khong co trong bang map_template");
      }
      this.startMapId = startMapId;
      Log.info("Da nap %d map %s, map khoi dau %d, tick %d ms tren %d luong", maps.size(), maps.keySet(), startMapId,
            tickMillis, tickThreads);
   }

   // ------------------------------------------------------------------ lọc NPC / quái / cây lúc nạp map

   /**
    * Bỏ NPC có npc_template.id không tồn tại (client nhận template null). Bóng mặc định: {@code talk_button} 0 = đồ vật
    * (Cây táo, Rương đồ) không bóng, còn lại (người) có bóng — cùng cờ nút nói chuyện của client mới.
    */
   private static List<MapTemplate.NpcSpawn> usableNpcs(MapTemplate map) {
      List<MapTemplate.NpcSpawn> kept = new ArrayList<>();
      for (MapTemplate.NpcSpawn npc : map.npcs()) {
         MapPack.Npc template = GameDataCache.npc(npc.id());
         if (template != null) {
            npc.applyDefaultShadow(template.talkButton());
            kept.add(npc);
         } else {
            Log.warn("Map %d: bo NPC id %d (khong co trong npc_template)", map.id(), npc.id());
         }
      }
      return kept;
   }

   /** Bỏ quái có mob_template.id lạ hoặc biến thể ngoài mob_variant (client đọc variants[idx] -> lỗi). */
   private static List<MapTemplate.MobSpawn> usableMobs(MapTemplate map) {
      List<MapTemplate.MobSpawn> kept = new ArrayList<>();
      for (MapTemplate.MobSpawn mob : map.mobs()) {
         MapPack.Mob template = GameDataCache.mob(mob.id());
         if (template == null) {
            Log.warn("Map %d: bo quai id %d (khong co trong mob_template)", map.id(), mob.id());
         } else if (mob.variant() < 0 || mob.variant() >= template.variants().size()) {
            Log.warn("Map %d: bo quai id %d bien the %d (mob_variant chi co %d)", map.id(), mob.id(), mob.variant(),
                  template.variants().size());
         } else {
            kept.add(mob);
         }
      }
      return kept;
   }

   /** Bỏ cây có plant_template.id không tồn tại. */
   private static List<MapTemplate.PlantSpawn> usablePlants(MapTemplate map) {
      List<MapTemplate.PlantSpawn> kept = new ArrayList<>();
      for (MapTemplate.PlantSpawn plant : map.plants()) {
         if (GameDataCache.plant(plant.id()) != null) {
            kept.add(plant);
         } else {
            Log.warn("Map %d: bo cay id %d (khong co trong plant_template)", map.id(), plant.id());
         }
      }
      return kept;
   }

   /**
    * Chỉ số / ngoại hình của người chơi vừa đổi (mặc đồ, đổi ngoại trang...): trên luồng khu, bỏ gói xuất hiện
    * đã mã hoá và phát khối chỉ số mới (opcode 23 sub 0) cho cả khu, kể cả chính họ.
    */
   /** Đổi tên: op 2 sub 7 cho cả khu (kể cả chính mình). Mọi luồng. */
   public void broadcastRename(PlayerData data) {
      WorldPlayer player = players.get(data.getId());
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      String name = data.name;
      zone.submit(() -> {
         if (player.zone == zone) {
            player.invalidateSpawnPacket();
            zone.broadcast(WorldPackets.rename(player.id(), name), null);
         }
      });
   }

   /** Chỉ skin đổi (ẩn / hiện trang phục...): op 2 sub 1 cho cả khu. Gọi sau {@code ItemService.refreshLook}. Mọi luồng. */
   public void broadcastSkins(PlayerData data) {
      WorldPlayer player = players.get(data.getId());
      Zone zone = player == null ? null : player.zone;
      com.tlnt.server.item.Appearance look = data.appearance;
      if (zone == null || look == null) {
         return;
      }
      String[] skins = look.skins();
      zone.submit(() -> {
         if (player.zone == zone) {
            player.invalidateSpawnPacket();
            zone.broadcast(WorldPackets.skins(player.id(), skins), null);
         }
      });
   }

   public void broadcastStats(PlayerData data) {
      WorldPlayer player = players.get(data.getId());
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            return;
         }
         player.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.playerStats(player), null);
         zone.sendPartyFlags(player); // khối chỉ số vừa đặt lại cờ tên vàng ở client
         zone.syncPet(player); // dùng / tháo / ẩn thú cưng -> con thú đi theo hiện / mất
      });
   }

   /** Chu kỳ phát vị trí người / thú cưng (ms, {@code world.moveBroadcastMillis}), tách khỏi chu kỳ tick. */
   public static void setMoveBroadcastMillis(long millis) {
      Zone.moveBroadcastMs = Math.max(1, millis);
   }


   /** Thống kê cho log định kỳ. */
   public String stats() {
      int zones = 0;
      for (GameMap map : maps.values()) {
         zones += map.zoneCount();
      }
      return "the gioi " + players.size() + " nguoi, " + zones + " khu | " + engine.takeStats();
   }

   /** Map đang chạy, null nếu không có. */
   GameMap map(int mapId) {
      return maps.get(mapId);
   }

   /** Mẫu map đang chạy, null nếu không có. */
   public MapTemplate mapTemplate(int mapId) {
      GameMap map = maps.get(mapId);
      return map == null ? null : map.template();
   }

   /** Mẫu các map thường (không riêng tư / phó bản) — bất biến, dùng chọn quái cho nhiệm vụ hằng ngày. */
   public List<MapTemplate> fieldMaps() {
      List<MapTemplate> list = new ArrayList<>();
      for (GameMap map : maps.values()) {
         if (!map.isPrivate() && !map.isDungeon()) {
            list.add(map.template());
         }
      }
      return list;
   }

   /** 1 lượt rải cây / tổ ong / quả dừa toàn server ({@link SpawnDirector}) — scheduler gọi mỗi giây. */
   public void runSpawns() {
      SpawnDirector.run(this, System.currentTimeMillis());
   }

   public boolean hasMap(int mapId) {
      return maps.containsKey(mapId);
   }

   public int onlineCount() {
      return players.size();
   }

   // ------------------------------------------------------------------ vào / ra thế giới

   /** Đưa nhân vật vào map đã lưu (hoặc map khởi đầu), khu số nhỏ nhất còn chỗ. Khu không được lưu. */
   public void enter(PlayerSession session, PlayerData data) {
      GameMap map = maps.get(data.mapId);
      boolean savedPosition = map != null && data.x != null && data.y != null;
      if (map == null) {
         map = maps.get(startMapId);
      }
      float x = savedPosition ? map.template().clampX(data.x) : map.template().spawnX();
      float y = savedPosition ? map.template().clampY(data.y) : map.template().spawnY();

      WorldPlayer player = new WorldPlayer(session, data);
      WorldPlayer previous = players.put(data.getId(), player);
      if (previous != null) { // không nên xảy ra (đăng nhập trùng đã kick trước), phòng hờ
         removeFromZone(previous);
      }
      if (map.isAltar()) { // thoát game trong Tế đàn: vào lại ở Làng (lượt còn giờ thì gặp Buraen để quay lại map đang dở)
         map = maps.get(startMapId);
         x = map.template().spawnX();
         y = map.template().spawnY();
      } else if (map.isThunder() || map.isArena()) { // thoát game ở Lôi vực / Giải đấu: vào lại ở Làng
         map = maps.get(startMapId);
         x = map.template().spawnX();
         y = map.template().spawnY();
      } else if (map.isLinhMoc()) { // thoát game trong Rừng Linh Mộc: vào lại cạnh Bác Rano (gặp để quay lại)
         map = maps.get(LinhMocRules.config().hubMapId());
         float[] hub = linhMocHub(map);
         x = hub[0];
         y = hub[1];
      }
      Zone zone = reserveNearParty(map, data); // đăng nhập: vào khu có đồng đội nếu có
      Zone target = zone;
      float fx = x;
      float fy = y;
      target.submit(() -> target.join(player, fx, fy));
   }

   /**
    * Người chơi giả ({@code bot/BotService}) vào map {@code data.mapId} tại ({@code data.x}, mặt đất dưới {@code data.y}):
    * khu số nhỏ nhất còn dưới trần bot riêng của khu (bốc trong khoảng 50–100% {@code maxBotsPerZone}) và còn chừa
    * {@code keepFree} chỗ cho người thật. Vào khu đi đúng đường {@link Zone#join} của người thật (người xung quanh thấy
    * xuất hiện như mọi người). @return false = hết chỗ
    */
   public boolean enterBot(PlayerSession session, PlayerData data, int maxBotsPerZone, int keepFree) {
      GameMap map = maps.get(data.mapId);
      if (map == null || map.isPrivate() || map.isDungeon() || map.isArena() || map.isThunder()) {
         return false;
      }
      Zone zone = map.reserveForBot(maxBotsPerZone, keepFree);
      if (zone == null) {
         return false;
      }
      MapTemplate t = map.template();
      float x = t.clampX(data.x == null ? t.spawnX() : data.x);
      float y = BossMover.groundUnder(t, x, t.clampY(data.y == null ? t.spawnY() : data.y));
      WorldPlayer player = new WorldPlayer(session, data);
      WorldPlayer previous = players.put(data.getId(), player);
      if (previous != null) {
         removeFromZone(previous);
      }
      zone.submit(() -> zone.join(player, x, y));
      return true;
   }

   /**
    * Như {@link #enterBot(PlayerSession, PlayerData, int, int)} nhưng ép khu {@code zoneId} (≥ 0; nhóm bot / bot săn
    * boss vào cùng khu). @return số khu đã giữ, -1 = không vào được
    */
   public int enterBot(PlayerSession session, PlayerData data, int zoneId, int maxBotsPerZone, int keepFree) {
      GameMap map = maps.get(data.mapId);
      if (map == null || map.isPrivate() || map.isDungeon() || map.isArena() || map.isThunder()) {
         return -1;
      }
      Zone zone = map.reserveForBot(zoneId, maxBotsPerZone, keepFree);
      if (zone == null) {
         return -1;
      }
      MapTemplate t = map.template();
      float x = t.clampX(data.x == null ? t.spawnX() : data.x);
      float y = BossMover.groundUnder(t, x, t.clampY(data.y == null ? t.spawnY() : data.y));
      WorldPlayer player = new WorldPlayer(session, data);
      WorldPlayer previous = players.put(data.getId(), player);
      if (previous != null) {
         removeFromZone(previous);
      }
      zone.submit(() -> zone.join(player, x, y));
      return zone.id();
   }

   /**
    * Bot bị người chơi đánh (PK): khu báo về đây trên luồng tick ({@code ZoneCombat.hurt}) — BotService gắn lúc khởi
    * động. Chỉ được chuyển việc đi, không làm nặng.
    */
   public interface BotHurtListener {
      void attacked(int botId, int attackerId);
   }

   static volatile BotHurtListener botHurt;

   public static void setBotHurtListener(BotHurtListener listener) {
      botHurt = listener;
   }

   /** Bot đánh trả người {@code targetId} cùng khu: chạy lại gần rồi báo kỹ năng nào với tới ({@link Zone#botChase}). */
   /** Bot săn boss thế giới người chơi giả mã {@code key} ({@link Zone#botChaseWorldBoss}). Mọi luồng. */
   public void botChaseWorldBoss(PlayerSession session, int key, int[] reachX, int[] reachY, boolean spare,
         OfflineReply reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         reply.accept(-1, 0);
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            reply.accept(-1, 0);
            return;
         }
         zone.botChaseWorldBoss(player, key, reachX, reachY, spare, reply);
      });
   }

   public void botChase(PlayerSession session, int targetId, int[] reachX, int[] reachY, OfflineReply reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         reply.accept(-1, 0);
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            reply.accept(-1, 0);
            return;
         }
         zone.botChase(player, targetId, reachX, reachY, reply);
      });
   }

   /**
    * Chỗ đứng câu cá được của map: bờ / bến cao bằng hoặc hơn mặt nước dọc từng vùng nước, cách nước ≤ {@code reachPx} (luật câu
    * {@code FishingService}). Đọc dữ liệu map bất biến — mọi luồng.
    */
   public List<float[]> fishingSpots(int mapId, float reachPx) {
      MapTemplate t = mapTemplate(mapId);
      List<float[]> out = new ArrayList<>();
      if (t == null || t.waters() == null) {
         return out;
      }
      for (MapTemplate.Water w : t.waters()) {
         float top = w.y() + w.h(); // mặt nước (trục y hướng lên)
         // Dò dọc vùng nước (cả 2 đầu ra ngoài 1 chút): chỗ có mặt đất (bờ / bến) cao bằng hoặc hơn mặt nước, cách nước
         // trong tầm câu. Vùng nước ghép từ ảnh nước hay trải gần hết bề rộng map -> không chỉ lấy 2 đầu (= mép map).
         for (float x = w.x() - reachPx * 0.5f; x <= w.x() + w.w() + reachPx * 0.5f; x += 130) {
            float cx = t.clampX(x);
            float gy = BossMover.groundUnder(t, cx, t.clampY(top + 300));
            if (gy >= top - 10 && gy <= top + reachPx * 0.6f && t.nearWater(cx, gy, reachPx * 0.8f)) {
               float waterX = cx < w.x() ? w.x() + 1 : cx > w.x() + w.w() ? w.x() + w.w() - 1
                     : (cx - w.x() < w.x() + w.w() - cx ? w.x() : w.x() + w.w()); // quay mặt về phía nước gần nhất
               out.add(new float[] {cx, gy, cx >= w.x() && cx <= w.x() + w.w() ? cx + (cx < waterX ? 1 : -1) * 50 : waterX});
            }
         }
      }
      return out;
   }

   /** Người chơi giả đứng chơi: chạy bộ tới hoành độ {@code x} cùng map ({@link Zone#botStroll}). */
   public void botStroll(PlayerSession session, float x) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone != null) {
         zone.submit(() -> zone.botStroll(player, x));
      }
   }

   /** Bot chạy bộ tới ({@code x}, mặt đất dưới {@code y}) — vd cửa dịch chuyển ở tầng khác ({@link Zone#botWalkTo}). */
   public void botWalkTo(PlayerSession session, float x, float y) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone != null) {
         zone.submit(() -> zone.botWalkTo(player, x, y));
      }
   }

   /** Map bot lữ khách được đi tới bằng cửa: map thường (không riêng / phó bản / đấu trường / Lôi vực / Tế đàn). */
   public boolean botCanVisit(int mapId) {
      GameMap map = maps.get(mapId);
      return map != null && !map.isPrivate() && !map.isDungeon() && !map.isArena() && !map.isThunder()
            && !map.isAltar() && !map.isLinhMoc();
   }

   /**
    * Bot lữ khách đi qua cửa thứ {@code position} của map đang đứng như người thật chạm cửa ({@link #useWaypoint}), nhưng giữ
    * chỗ theo trần bot ({@link GameMap#reserveForBot} — đếm {@code Zone.botSlots} đúng khi rời khu). {@code reply} nhận
    * map mới hoặc -1 (chưa tới cửa / map khoá / hết chỗ), chạy trên luồng khu.
    */
   public void botUseDoor(PlayerSession session, int position, int maxBots, int keepFree,
         java.util.function.IntConsumer reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         reply.accept(-1);
         return;
      }
      zone.submit(() -> {
         MapTemplate from = zone.map().template();
         MapTemplate.Waypoint waypoint = from.waypointAt(position);
         if (player.zone != zone || player.locked || player.data().isDead() || waypoint == null
               || waypoint.targetMapId() == null || !waypoint.contains(player.x, player.y, WAYPOINT_REACH_PX)
               || System.currentTimeMillis() - player.lastTransferAt < TRANSFER_COOLDOWN_MS
               || player.duelOpponentId != 0 || !botCanVisit(waypoint.targetMapId())) {
            reply.accept(-1);
            return;
         }
         GameMap target = maps.get(waypoint.targetMapId());
         Zone to = target.reserveForBot(maxBots, keepFree);
         if (to == null) {
            reply.accept(-1);
            return;
         }
         float[] arrival = arrivalPoint(target.template(), from.id(), waypoint);
         transfer(player, zone, target, to, arrival[0], arrival[1]);
         reply.accept(target.template().id());
      });
   }

   /** Bot đổi sang khu khác cùng map (như bấm bảng Khu vực), giữ chỗ theo trần bot. {@code reply}: khu mới / -1. */
   public void botChangeZone(PlayerSession session, int maxBots, int keepFree, java.util.function.IntConsumer reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         reply.accept(-1);
         return;
      }
      zone.submit(() -> {
         GameMap map = zone.map();
         if (player.zone != zone || player.locked || player.data().isDead() || player.duelOpponentId != 0
               || System.currentTimeMillis() - player.lastTransferAt < TRANSFER_COOLDOWN_MS
               || !botCanVisit(map.template().id())) {
            reply.accept(-1);
            return;
         }
         Zone to = map.reserveForBotExcept(zone.id(), maxBots, keepFree);
         if (to == null) {
            reply.accept(-1);
            return;
         }
         transfer(player, zone, map, to, player.x, player.y);
         reply.accept(to.id());
      });
   }

   /** Người thật gần nhất trong {@code radiusPx} quanh bot: {@code reply(id, x, y)}, không có thì id -1. Luồng khu. */
   public interface HumanReply {
      void accept(int id, float x, float y);
   }

   public void botFindHuman(PlayerSession session, int preferId, float radiusPx, HumanReply reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         reply.accept(-1, 0, 0);
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            reply.accept(-1, 0, 0);
            return;
         }
         WorldPlayer best = zone.nearestHuman(player, preferId, radiusPx);
         if (best == null) {
            reply.accept(-1, 0, 0);
         } else {
            reply.accept(best.id(), best.x, best.y);
         }
      });
   }

   /**
    * 2 bot cùng khu thách đấu nhau (đấu tập): như người bấm "Thách đấu" + "Đồng ý" ({@link #startDuel}) — HP dừng ở 1,
    * không ai chết. Xa nhau quá {@code rangePx} thì {@link #startDuel} tự bỏ.
    */
   public void botDuel(PlayerSession a, int opponentId, float rangePx, long durationMs) {
      WorldPlayer pa = find(a);
      WorldPlayer pb = players.get(opponentId);
      if (pa != null && pb != null && pa.zone != null && pa.zone == pb.zone) {
         startDuel(pa, pb, rangePx, durationMs);
      }
   }

   /** Đang trong trận thách đấu (đọc gần đúng từ luồng khác). */
   public boolean inDuel(int playerId) {
      WorldPlayer p = players.get(playerId);
      return p != null && p.duelOpponentId != 0;
   }

   /**
    * Giữ chỗ cho {@code data} ở {@code map}: map Tế đàn -> khu của lượt đang chạy (null = không có lượt nào đang chạy);
    * map khác -> {@link GameMap#reserveFor}.
    */
   /**
    * Vào map thường: có đồng đội (tổ đội) đang ở map này thì vào ĐÚNG khu của đồng đội (trưởng nhóm trước, khu đầy thì
    * người kế); không có / khu đầy hết -> khu số nhỏ nhất còn chỗ ({@link GameMap#reserveFor}). Map riêng / phó bản / đài
    * đấu như cũ. Mọi luồng.
    */
   Zone reserveNearParty(GameMap map, PlayerData data) {
      if (data.party != null && !map.isPrivate() && !map.isAltar() && !map.isLinhMoc() && !map.isArena()) {
         for (int memberId : com.tlnt.server.party.PartyService.memberIdsOf(data)) {
            WorldPlayer mate = memberId == data.getId() ? null : players.get(memberId);
            Zone at = mate == null ? null : mate.zone;
            if (at != null && at.map() == map) {
               Zone reserved = map.reserveZone(at.id());
               if (reserved != null) {
                  return reserved;
               }
            }
         }
      }
      return map.reserveFor(data.getId());
   }

   private Zone reserve(GameMap map, PlayerData data) {
      if (map.isLinhMoc()) {
         LinhMocHooks hooks = linhMocHooks;
         LinhMocRun run = hooks == null ? null : hooks.activeRun(data.getId());
         return run == null ? null : map.reserveLinhMoc(run);
      }
      if (!map.isAltar()) {
         return reserveNearParty(map, data);
      }
      AltarRun state = data.altarRun;
      long start = state.start;
      long end = state.activeEnd(System.currentTimeMillis());
      if (end == 0) {
         return null;
      }
      int zoneNo = altarZones.zoneOf(data.getId(), start, end);
      return map.reserveAltar(data.getId(), zoneNo, start, end);
   }

   /** Thoát game / mất kết nối / bị kick. Gọi nhiều lần vẫn an toàn. */
   public void leave(PlayerData data) {
      WorldPlayer player = players.remove(data.getId());
      if (player != null) {
         removeFromZone(player);
      }
   }

   private static void removeFromZone(WorldPlayer player) {
      player.removed = true;
      Zone zone = player.zone;
      if (zone != null) {
         zone.submit(() -> zone.leave(player));
      }
      // zone == null: đang chuyển khu -> lệnh join ở khu mới thấy removed và tự huỷ chỗ đã giữ.
   }

   /** Admin panel: ảnh chụp mọi người đang trong thế giới (đọc gần đúng, mọi luồng). */
   public List<WorldPlayer> onlinePlayers() {
      List<WorldPlayer> list = new ArrayList<>(players.size());
      for (WorldPlayer p : players.values()) {
         if (!p.removed) {
            list.add(p);
         }
      }
      return list;
   }

   /** Admin panel: mỗi map {id, tên, số khu đang mở, số người} (đếm người theo khu hiện tại, gần đúng). */
   public List<Object[]> mapStats() {
      Map<Integer, Integer> count = new HashMap<>();
      for (WorldPlayer p : players.values()) {
         Zone zone = p.zone;
         if (zone != null) {
            count.merge(zone.map().template().id(), 1, Integer::sum);
         }
      }
      List<Object[]> list = new ArrayList<>();
      for (GameMap map : maps.values()) {
         int id = map.template().id();
         list.add(new Object[] {id, map.template().name(), map.zoneCount(), count.getOrDefault(id, 0)});
      }
      list.sort((a, b) -> Integer.compare((Integer) a[0], (Integer) b[0]));
      return list;
   }

   /** Admin panel: vị trí hiện tại của cây hái được, tổ ong và quả dừa đang còn trong các khu cố định. */
   public List<Map<String, Object>> adminGatherLocations() {
      java.util.concurrent.ConcurrentLinkedQueue<Map<String, Object>> rows = new java.util.concurrent.ConcurrentLinkedQueue<>();
      List<CompletableFuture<Void>> pending = new ArrayList<>();
      for (GameMap map : maps.values()) {
         for (Zone zone : map.permanentZones()) {
            CompletableFuture<Void> done = new CompletableFuture<>();
            pending.add(done);
            zone.submit(() -> {
               try {
                  int mapId = map.template().id();
                  String mapName = map.template().name();
                  for (ZonePlants.Plant plant : zone.plants().alive()) {
                     Map<String, Object> row = new LinkedHashMap<>();
                     row.put("kind", "plant"); row.put("name", plantName(plant.templateId)); row.put("templateId", plant.templateId);
                     row.put("entityId", plant.id); row.put("origin", plant.fixedIndex >= 0 ? "fixed"
                           : plant.fixedIndex == ZonePlants.ADMIN ? "admin" : "random");
                     row.put("mapId", mapId); row.put("mapName", mapName); row.put("zone", zone.id());
                     row.put("x", Math.round(plant.x * 10f) / 10.0); row.put("y", Math.round(plant.y * 10f) / 10.0);
                     rows.add(row);
                  }
                  for (ZoneMobs.Gather gather : zone.mobs().gathers()) {
                     Map<String, Object> row = new LinkedHashMap<>();
                     row.put("kind", gather.entry.mobId() == GatherConfig.HIVE_MOB ? "hive" : "coconut");
                     row.put("name", mobName(gather.entry.mobId())); row.put("templateId", gather.entry.mobId());
                     row.put("entityId", gather.mob.id()); row.put("origin", gather.admin ? "admin" : "random");
                     row.put("mapId", mapId); row.put("mapName", mapName); row.put("zone", zone.id());
                     row.put("x", Math.round(gather.mob.x() * 10f) / 10.0); row.put("y", Math.round(gather.mob.y() * 10f) / 10.0);
                     row.put("hp", gather.hp); row.put("maxHp", gather.entry.hp());
                     rows.add(row);
                  }
               } finally {
                  done.complete(null);
               }
            });
         }
      }
      try {
         CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(2, TimeUnit.SECONDS);
      } catch (Exception ignored) {
         // Trả về snapshot từng phần nếu một khu chưa xử lý được lệnh trong thời hạn admin.
      }
      List<Map<String, Object>> result = new ArrayList<>(rows);
      result.sort(java.util.Comparator.comparingInt((Map<String, Object> r) -> (Integer) r.get("mapId"))
            .thenComparingInt(r -> (Integer) r.get("zone")).thenComparing(r -> (String) r.get("name")));
      return result;
   }

   /** Người chơi đang trong thế giới theo id (bất kỳ map nào), null = offline / đang thoát. */
   public WorldPlayer online(int playerId) {
      WorldPlayer player = players.get(playerId);
      return player == null || player.removed ? null : player;
   }

   /** Người chơi của session (đã vào thế giới), null nếu chưa / đã thoát. */
   public WorldPlayer online(PlayerSession session) {
      WorldPlayer player = find(session);
      return player == null || player.removed ? null : player;
   }

   private WorldPlayer find(PlayerSession session) {
      PlayerData data = session.getPlayer();
      return data == null ? null : players.get(data.getId());
   }

   /**
    * Tổ đội (không trùng, theo thứ tự người trong khu) của mọi người đang ở cùng khu với người gửi. Đọc danh sách người
    * trên luồng tick của khu — tối đa {@code max_players_per_zone} người — thay vì duyệt mọi tổ đội cả server.
    * {@code reply} chạy trên luồng tick: chỉ chuyển kết quả về luồng khác, đừng làm việc nặng / khoá lâu trong đó.
    */
   public void partiesInZone(PlayerSession session, Consumer<List<Party>> reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            return;
         }
         Set<Party> found = new LinkedHashSet<>();
         for (WorldPlayer p : zone.players()) {
            Party party = p.data().party;
            if (party != null) {
               found.add(party);
            }
         }
         reply.accept(new ArrayList<>(found));
      });
   }

   // ------------------------------------------------------------------ di chuyển

   /** Opcode 5 sub 0. Chạy ngay trên luồng mạng, chỉ ghi lại vị trí mới nhất (tick sẽ xử lý). */
   public void move(PlayerSession session, float x, float y) {
      WorldPlayer player = find(session);
      if (player != null && player.zone != null && Float.isFinite(x) && Float.isFinite(y)) {
         player.reportPosition(x, y);
      }
   }

   // ------------------------------------------------------------------ khoá nhân vật (opcode 2 sub 1)

   /**
    * Nút MountButton: bật/tắt chế độ khoá nhân vật (đứng yên, ẩn HUD, camera tự do). Chạy trên luồng tick
    * của khu -> đọc/ghi {@link WorldPlayer#locked} không cần khoá. Gói trạng thái encode 1 lần rồi phát
    * cho cả khu (kể cả chính mình để client cập nhật HUD). Bấm trùng trạng thái hoặc quá nhanh: bỏ qua.
    */
   public void setLocked(PlayerSession session, boolean locked) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || player.locked == locked) {
            return;
         }
         long now = System.currentTimeMillis();
         if (now - player.lastLockToggleAt < LOCK_TOGGLE_COOLDOWN_MS) {
            player.send(WorldPackets.lockState(player.id(), player.locked)); // trả lại trạng thái thật
            return;
         }
         player.lastLockToggleAt = now;
         player.locked = locked;
         player.invalidateSpawnPacket();
         player.clearPendingPosition();
         zone.broadcast(WorldPackets.lockState(player.id(), locked), null);
      });
   }

   // ------------------------------------------------------------------ trạng thái PK (opcode 42 sub 0)

   /**
    * Người chơi chọn trạng thái PK ở menu biểu tượng ({@code PopupMenu}). Luật đổi: {@link PkRules#checkChange}
    * (chờ giữa 2 lần đổi, cờ Thử thách phải giữ tối thiểu, map cấm PK). Chạy trên luồng tick của khu
    * (cùng luồng dựng gói xuất hiện có byte trạng thái). Đổi được -> phát op 42 sub 0 cho cả khu, kể cả chính mình
    * (client đổi biểu tượng khi nhận gói); bị từ chối -> gửi lại trạng thái thật cho riêng người đó.
    */
   public void setFaction(PlayerSession session, int wanted) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            return;
         }
         PlayerData d = player.data();
         if (wanted == d.faction) {
            return;
         }
         long now = System.currentTimeMillis();
         String error = d.hp <= 0 ? "Đã tử vong, không thể đổi trạng thái chiến đấu."
               : player.duelOpponentId != 0 ? "Đang thách đấu, không thể đổi trạng thái chiến đấu."
               : zone.map().isThunder() ? "Không thể đổi trạng thái chiến đấu ở Lôi vực Sấm sét."
               : zone.map().isArena() ? "Không thể đổi trạng thái chiến đấu ở đấu trường."
               : wanted != PkRules.PEACE && ItemBuffs.active(d, ItemBuffs.OPTION_PK_SHIELD, now)
                     ? "Đang dùng Bùa hộ giáp, không thể đổi sang trạng thái chiến đấu khác."
               : PkRules.checkChange(d.faction, wanted, d.factionChangedAt, now, zone.map().template().id());
         if (error != null) {
            player.send(Packets.notice(error));
            player.send(WorldPackets.faction(player.id(), d.faction));
            return;
         }
         d.faction = (byte) wanted;
         d.factionChangedAt = now;
         player.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.faction(player.id(), d.faction).toPacket(), null);
      });
   }

   /**
    * Bùa hộ giáp vừa dùng (luồng session): về cờ Hoà bình NGAY (bỏ qua thời gian chờ đổi cờ), phát op 42 sub 0 cho cả khu.
    * Khi còn bùa, {@link #setFaction} không cho đổi sang cờ khác.
    */
   public void forcePeace(PlayerSession session) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         PlayerData d = player.data();
         if (player.zone != zone || d.faction == PkRules.PEACE || zone.map().isThunder() || zone.map().isArena()) {
            return;
         }
         d.faction = PkRules.PEACE;
         d.factionChangedAt = System.currentTimeMillis();
         player.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.faction(player.id(), d.faction).toPacket(), null);
      });
   }

   /** Túi sữa dê (luồng session): tắt đóng băng / trói đang hiện trên người chơi này cho cả khu (op 35, bool false). */
   public void clearPlayerStatus(PlayerSession session) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone) {
            zone.broadcast(WorldPackets.playerStatusOff(MobStatus.ICE, player.id()).toPacket(), null);
            zone.broadcast(WorldPackets.playerStatusOff(MobStatus.STUN, player.id()).toPacket(), null);
            zone.broadcast(WorldPackets.playerStatusOff(MobStatus.WEAKEN, player.id()).toPacket(), null);
            zone.broadcast(WorldPackets.playerStatusOff(MobStatus.BURN, player.id()).toPacket(), null);
            zone.broadcast(WorldPackets.playerStatusOff(MobStatus.ROOT, player.id()).toPacket(), null);
         }
      });
   }

   // ------------------------------------------------------------------ thách đấu (opcode 41)

   /**
    * Bắt đầu thách đấu giữa 2 người (người được mời đã bấm "Đồng ý"). Chạy trên luồng tick khu của {@code a}: cả 2
    * phải còn cùng khu, đứng gần, không ai đang thách đấu. Vào trận NGAY (không đếm ngược op 41 sub 3), hết
    * {@code durationMs} thì kết thúc. Rời khu = kết thúc ({@link Zone#leave}).
    *
    * <p>Lúc "Bắt đầu": bật cờ đấu (op 41 sub 0 + khối chỉ số) cho cả khu -> client 2 người chọn nhau làm mục tiêu
    * được bất kể trạng thái PK ({@link PkRules#canAttack} tham số {@code dueling}). Trong trận HP không xuống dưới 1:
    * về 1 = thua ({@link ZoneCombat}), không ai chết.
    */
   public void startDuel(WorldPlayer a, WorldPlayer b, float rangePx, long durationMs) {
      Zone zone = a.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (a.zone != zone || b.zone != zone) {
            a.send(Packets.notice("Đối phương không còn ở gần."));
            return;
         }
         if (zone.arena != null) {
            a.send(Packets.notice("Không thể thách đấu ở đấu trường."));
            return;
         }
         if (a.data().isDead() || b.data().isDead()) {
            a.send(Packets.notice("Không thể thách đấu khi đã bị hạ gục."));
            return;
         }
         if (a.duelOpponentId != 0 || b.duelOpponentId != 0) {
            a.send(Packets.notice("Một trong hai người đang thách đấu."));
            return;
         }
         if (Math.abs(a.x - b.x) > rangePx || Math.abs(a.y - b.y) > rangePx) {
            a.send(Packets.notice("Khoảng cách quá xa, không thể thách đấu."));
            b.send(Packets.notice("Khoảng cách quá xa, không thể thách đấu."));
            return;
         }
         long id = ++zone.duelSeq;
         a.duelOpponentId = b.id();
         b.duelOpponentId = a.id();
         a.duelId = id;
         b.duelId = id;
         // Không đếm ngược 3-2-1-Bắt đầu: đồng ý là vào trận ngay (bật cờ đấu cho cả khu)
         a.duelActive = true;
         b.duelActive = true;
         a.invalidateSpawnPacket();
         b.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.duelFlag(a.id(), true, b.id()), null);
         zone.broadcast(WorldPackets.duelFlag(b.id(), true, a.id()), null);
         zone.schedule(durationMs, () -> {
            if (a.duelId == id && b.duelId == id) {
               endDuel(zone, a, "Hết giờ thách đấu.");
            }
         });
      });
   }

   /** Kết thúc thách đấu của {@code p} (và đối thủ nếu còn trong khu), cùng 1 câu báo cho cả 2. Luồng tick khu. */
   static void endDuel(Zone zone, WorldPlayer p, String reason) {
      WorldPlayer other = p.duelOpponentId == 0 ? null : zone.find(p.duelOpponentId);
      endDuel(zone, p, reason, other, reason);
   }

   /** {@code winner} hạ HP {@code loser} về 1. Luồng tick khu. */
   static void duelWon(Zone zone, WorldPlayer winner, WorldPlayer loser) {
      endDuel(zone, winner, "Xuất sắc! Bạn đã chiến thắng vòng đấu", loser, "Bạn đã bị đánh bại! Hãy luyện tập thêm");
      com.tlnt.server.daily.DailyEvents.add(winner, com.tlnt.server.daily.DailyKind.PVP, 0, 1); // NV hằng ngày "Đánh bại người khác"
   }

   private static void endDuel(Zone zone, WorldPlayer p, String reason, WorldPlayer other, String otherReason) {
      if (p.duelOpponentId == 0) {
         return;
      }
      int opponentId = p.duelOpponentId;
      clearDuel(zone, p);
      p.send(Packets.notice(reason));
      if (other != null && other.id() == opponentId && other.duelOpponentId == p.id()) {
         clearDuel(zone, other);
         other.send(Packets.notice(otherReason));
      }
   }

   private static void clearDuel(Zone zone, WorldPlayer p) {
      boolean wasActive = p.duelActive;
      p.duelOpponentId = 0;
      p.duelId = 0;
      p.duelActive = false;
      if (wasActive) {
         p.invalidateSpawnPacket();
         Packet flag = WorldPackets.duelFlag(p.id(), false, 0);
         zone.broadcast(flag, null);
         if (zone.find(p.id()) == null) { // đang rời khu: vẫn báo cho chính họ
            p.send(flag);
         } else {
            zone.sendPartyFlags(p); // còn trong tổ đội: đồng đội vẫn thấy tên vàng
         }
      }
   }

   // ------------------------------------------------------------------ bị hạ gục / hồi sinh (opcode 28)

   /**
    * Hồi sinh tại chỗ ({@code percent}% HP tối đa; 100 = đầy cả HP lẫn MP) — người chơi đã trả giá (cỏ hồi sinh) trên luồng session. Luồng tick
    * khu: đặt HP, phát hiệu ứng hồi sinh + HP mới cho cả khu (client của người khác thấy sống lại).
    */
   public void reviveHere(PlayerSession session, int percent) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || !player.data().isDead()) {
            return;
         }
         revive(player, percent);
         broadcastRevive(zone, player);
      });
   }

   /**
    * "Về làng": hồi sinh đầy HP / MP ở điểm xuất hiện của map khởi đầu — trong Rừng Linh Mộc: map đầu của lượt (khu
    * riêng của lượt).
    */
   public void reviveInVillage(PlayerSession session) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || !player.data().isDead()) {
            return;
         }
         revive(player, 100);
         GameMap first = zone.linhMoc != null ? maps.get(LinhMocRules.firstMap()) : null;
         // Rừng Linh Mộc: hồi sinh ở map đầu của lượt (hết lượt / khu đầy thì về làng)
         Zone dungeon = first == null ? null : reserve(first, player.data());
         GameMap village = dungeon != null ? first : maps.get(startMapId);
         Zone target = dungeon != null ? dungeon : village.reserveFor(player.id());
         transfer(player, zone, village, target, village.template().spawnX(), village.template().spawnY());
         target.submit(() -> { // xếp SAU lệnh vào khu (cùng hàng đợi) -> người trong khu đã thấy mình
            if (player.zone == target) {
               broadcastRevive(target, player);
            }
         });
      });
   }

   private static void revive(WorldPlayer player, int percent) {
      PlayerData d = player.data();
      PlayerStats stats = d.stats;
      d.setHpMp(Math.max(1, stats.maxHp * percent / 100), percent >= 100 ? stats.maxMp : d.mp);
      player.invalidateSpawnPacket();
      player.lives++; // Nanh Hổ răng kiếm: lượt sống mới = "mục tiêu mới"
      ZoneCombat.onRevive(player); // Bùa linh mộc
   }

   private static void broadcastRevive(Zone zone, WorldPlayer player) {
      PlayerData d = player.data();
      zone.broadcast(WorldPackets.revived(player.id()), null);
      zone.broadcast(ItemPackets.hpMp(player.id(), d.hp, d.stats.maxHp, d.mp, d.stats.maxMp).toPacket(), null);
      player.send(WorldPackets.playerStats(player)); // EXP có thể vừa bị trừ lúc bị hạ gục (điểm PK)
      zone.sendPartyFlags(player); // client tắt cờ tên vàng khi chết
   }

   /**
    * Phát {@code packet} cho mọi người trong khu đang chứa người chơi của {@code session}, kể cả chính họ. Đang chuyển
    * khu (chưa có khu) thì chỉ gửi cho chính họ.
    */
   public void broadcastAround(PlayerSession session, Packet packet) {
      WorldPlayer player = find(session);
      if (player == null) {
         return;
      }
      Zone zone = player.zone;
      if (zone == null) {
         player.send(packet);
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone) {
            zone.broadcast(packet, null);
         } else {
            player.send(packet);
         }
      });
   }

   // ------------------------------------------------------------------ chat khu (opcode 26 sub 0)

   /**
    * Phát tin chat cho mọi người trong khu, KỂ CẢ người nói (client không tự hiện tin của mình, chờ server gửi lại).
    * Mã hoá 1 lần. Đang khoá nhân vật vẫn chat được.
    */
   public void chat(PlayerSession session, String text) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone) {
            zone.broadcast(WorldPackets.chat(player.id(), text), null);
         }
      });
   }

   // ------------------------------------------------------------------ đánh quái (opcode 21 sub 0)

   /** Đòn nhắm 1 quái (1 hoặc nhiều gói, có thể kèm chảy máu) — xem {@link ZoneCombat#strike}. */
   public void strike(PlayerSession session, ZoneCombat.Strike strike) {
      onPlayerZone(session, (zone, player) -> ZoneCombat.strike(zone, player, strike));
   }

   /** Đòn chính + mục tiêu phụ quanh nó — xem {@link ZoneCombat#splash}. */
   public void splash(PlayerSession session, ZoneCombat.Splash splash) {
      onPlayerZone(session, (zone, player) -> ZoneCombat.splash(zone, player, splash));
   }

   /** Lướt + ảnh ảo — xem {@link ZoneCombat#illusion}. */
   public void illusion(PlayerSession session, ZoneCombat.Illusion illusion) {
      onPlayerZone(session, (zone, player) -> ZoneCombat.illusion(zone, player, illusion));
   }

   /** Xoay chém quanh người nhiều vòng — xem {@link ZoneCombat#spin}. */
   public void spin(PlayerSession session, ZoneCombat.Spin spin) {
      onPlayerZone(session, (zone, player) -> ZoneCombat.spin(zone, player, spin));
   }

   /** Gồng rồi bổ 1 quái — xem {@link ZoneCombat#charge}. */
   public void charge(PlayerSession session, ZoneCombat.Charge charge) {
      onPlayerZone(session, (zone, player) -> ZoneCombat.charge(zone, player, charge));
   }

   /** Vào thế {@code durationMs} rồi kết thúc — xem {@link ZoneCombat#channel}. */
   public void channel(PlayerSession session, int skillId, long durationMs) {
      onPlayerZone(session, (zone, player) -> ZoneCombat.channel(zone, player, skillId, durationMs));
   }

   /**
    * Chạy {@code action} trên luồng tick của khu đang chứa người chơi. Bỏ nếu đã rời khu trước khi tới lượt hoặc đang
    * khoá nhân vật (chế độ ngắm cảnh không đánh được) hoặc đã bị hạ gục.
    */
   private void onPlayerZone(PlayerSession session, BiConsumer<Zone, WorldPlayer> action) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone && !player.locked && !player.data().isDead()) {
            action.accept(zone, player);
         }
      });
   }

   // ------------------------------------------------------------------ hái cây (opcode 54)

   /** Hái xong (luồng tick khu): nơi gọi tự chuyển việc cho đồ vào túi sang luồng session. */
   public interface HarvestDone {
      void accept(WorldPlayer player, PlantConfig.Entry entry);
   }

   /**
    * Bấm hái cây {@code plantId}: trên luồng tick khu kiểm tra cây còn, có cấu hình hái, không ai đang hái, đứng trong
    * {@code reachPx}; được thì GIỮ cây cho người này, gửi thanh "Đang thu hoạch..." (op 54 sub 2), hẹn
    * {@code harvestMs} sau xong. Lúc xong kiểm tra lại (còn trong khu, còn sống, không khoá, chưa đi xa) rồi mới gỡ cây
    * (op 54 sub 3 cả khu) và gọi {@code done}.
    */
   public void harvestPlant(PlayerSession session, int plantId, float reachPx, HarvestDone done) {
      onPlayerZone(session, (zone, player) -> startHarvest(zone, player, plantId, reachPx, done));
   }

   private static void startHarvest(Zone zone, WorldPlayer p, int plantId, float reachPx, HarvestDone done) {
      if (p.harvestPlantId >= 0) {
         return; // đang hái cây khác (client khoá di chuyển nhưng vẫn bấm được)
      }
      long now = System.currentTimeMillis();
      ZonePlants.Plant plant = zone.plants().get(plantId);
      PlantConfig.Entry entry = plant == null ? null : PlantConfig.current().entry(plant.templateId);
      String error = plant == null ? "Cây này đã được thu hoạch."
            : entry == null || entry.items().isEmpty() ? "Chưa thể thu hoạch cây này."
            : plant.claimedByOther(p.id(), now) ? "Cây này đang có người thu hoạch."
            : !near(p, plant, reachPx) ? "Hãy lại gần hơn để thu hoạch."
            : null;
      if (error != null) {
         p.send(Packets.notice(error));
         return;
      }
      long seq = ++p.harvestSeq;
      plant.harvesterId = p.id();
      plant.claimUntil = now + entry.harvestMs() + 2_000;
      p.harvestPlantId = plant.id;
      p.send(PlantPackets.progress(0, entry.harvestMs()));
      zone.schedule(entry.harvestMs(), () -> finishHarvest(zone, p, plant, seq, reachPx, done));
   }

   private static void finishHarvest(Zone zone, WorldPlayer p, ZonePlants.Plant plant, long seq, float reachPx,
         HarvestDone done) {
      if (p.harvestSeq != seq || p.harvestPlantId != plant.id) {
         return; // đã huỷ / lượt cũ
      }
      if (p.zone != zone) {
         releaseClaim(zone, p);
         return;
      }
      PlantConfig.Entry entry = PlantConfig.current().entry(plant.templateId);
      String problem = zone.plants().get(plant.id) != plant ? "Cây đã biến mất."
            : p.data().isDead() ? "Đã bị hạ gục, thu hoạch thất bại."
            : p.locked ? "Đã huỷ thu hoạch."
            : !near(p, plant, reachPx) ? "Bạn đã rời xa cây, thu hoạch thất bại."
            : entry == null || entry.items().isEmpty() ? "Chưa thể thu hoạch cây này."
            : null;
      if (problem != null) {
         cancelHarvest(zone, p, problem);
         return;
      }
      p.harvestPlantId = -1;
      plant.harvesterId = 0;
      zone.plants().harvested(plant, p.id(), System.currentTimeMillis(), zone.broadcastAll());
      done.accept(p, entry);
   }

   /** Thôi hái (rời khu, bị huỷ): nhả cây, tắt thanh thu hoạch (op 54 sub 4). Luồng tick khu. */
   static void cancelHarvest(Zone zone, WorldPlayer p, String notice) {
      if (p.harvestPlantId < 0) {
         return;
      }
      releaseClaim(zone, p);
      p.send(PlantPackets.stop());
      if (notice != null) {
         p.send(Packets.notice(notice));
      }
   }

   private static void releaseClaim(Zone zone, WorldPlayer p) {
      ZonePlants.Plant plant = zone.plants().get(p.harvestPlantId);
      if (plant != null && plant.harvesterId == p.id()) {
         plant.harvesterId = 0;
      }
      p.harvestPlantId = -1;
      p.harvestSeq++;
   }

   private static boolean near(WorldPlayer p, ZonePlants.Plant plant, float reachPx) {
      return Math.abs(p.x - plant.x) <= reachPx && Math.abs(p.y - plant.y) <= reachPx;
   }

   // ---- lệnh test (/cay...)

   /** Thống kê cây của khu đang đứng -> thông báo cho chính người gõ. */
   public void plantInfo(PlayerSession session) {
      onZone(session, (zone, p) -> {
         ZonePlants plants = zone.plants();
         int mapId = zone.map().template().id();
         StringBuilder sb = new StringBuilder("Map " + mapId + " khu " + zone.id() + ": " + plants.spotCount()
               + " chỗ mọc.");
         for (PlantConfig.Entry e : PlantConfig.current().forMap(mapId)) {
            sb.append(' ').append(plantName(e.templateId())).append(' ').append(plants.countRandom(e.templateId()))
                  .append('/').append(e.max()).append(" (server ").append(ZonePlants.serverCount(e.templateId()))
                  .append(e.total() > 0 ? "/" + e.total() : "").append(", chờ hồi ")
                  .append(SpawnQuota.plant(e.templateId()).cooling(System.currentTimeMillis())).append(')');
            sb.append('.');
         }
         ZonePlants.Plant nearest = null;
         float best = Float.MAX_VALUE;
         int total = 0;
         for (ZonePlants.Plant plant : plants.alive()) {
            total++;
            float d = Math.abs(plant.x - p.x) + Math.abs(plant.y - p.y);
            if (d < best) {
               best = d;
               nearest = plant;
            }
         }
         sb.append(" Tổng ").append(total).append(" cây");
         if (nearest != null) {
            sb.append(", gần nhất: ").append(plantName(nearest.templateId)).append(" cách ")
                  .append(Math.round(Math.abs(nearest.x - p.x))).append(" px ngang");
         }
         sb.append('.');
         testLog(p, sb.toString());
      });
   }

   /** Lệnh test: bỏ thời gian hồi của mọi cây toàn server + rải ngay; cây đặt tay trong khu mọc lại ngay. */
   public void plantRespawnNow(PlayerSession session) {
      for (PlantConfig.Entry e : PlantConfig.current().byTemplate().values()) {
         SpawnQuota.plant(e.templateId()).clearCooling();
      }
      runSpawns();
      onZone(session, (zone, p) -> {
         zone.plants().update(System.currentTimeMillis(), true, zone.broadcastAll());
         testLog(p, "Đã bỏ thời gian chờ, cây được rải lại ngẫu nhiên toàn server. Gõ /caydau để xem ở đâu.");
      });
   }

   /** Lệnh test: cây ngẫu nhiên đang nằm ở map / khu nào (toàn server). */
   public void plantWhere(PlayerSession session) {
      StringBuilder sb = new StringBuilder();
      for (PlantConfig.Entry e : PlantConfig.current().byTemplate().values()) {
         SpawnQuota q = SpawnQuota.plant(e.templateId());
         sb.append(plantName(e.templateId())).append(": ").append(where(q, e.maps()));
      }
      WorldPlayer player = find(session);
      if (player != null) {
         testLog(player, sb.length() == 0 ? "Chưa cấu hình cây." : sb.toString());
      }
   }

   /** "map 1 khu 3, map 2 khu 0 ×2. " — đọc sổ đếm từng khu (an toàn mọi luồng). */
   private String where(SpawnQuota q, java.util.Collection<Integer> mapIds) {
      StringBuilder sb = new StringBuilder();
      for (int mapId : new java.util.TreeSet<>(mapIds)) {
         GameMap map = maps.get(mapId);
         if (map == null) {
            continue;
         }
         for (Zone zone : map.permanentZones()) {
            int n = q.inZone(zone);
            if (n > 0) {
               sb.append(sb.length() == 0 ? "" : ", ").append("map ").append(mapId).append(" khu ").append(zone.id())
                     .append(n > 1 ? " ×" + n : "");
            }
         }
      }
      return (sb.length() == 0 ? "không có" : sb.toString()) + ". ";
   }

   /** Dịch chuyển tới cạnh 1 cây ngẫu nhiên trong khu. */
   public void teleportToPlant(PlayerSession session) {
      onZone(session, (zone, p) -> {
         List<ZonePlants.Plant> list = new ArrayList<>();
         for (ZonePlants.Plant plant : zone.plants().alive()) {
            list.add(plant);
         }
         if (list.isEmpty()) {
            testLog(p, "Khu này không có cây nào.");
            return;
         }
         ZonePlants.Plant plant = list.get(ThreadLocalRandom.current().nextInt(list.size()));
         float x = zone.map().template().clampX(plant.x - 60);
         float y = plant.y + 5;
         p.x = x;
         p.y = y;
         p.data().x = x;
         p.data().y = y;
         p.lastMoveAt = System.currentTimeMillis();
         p.invalidateSpawnPacket();
         p.send(WorldPackets.teleportSelf(x, y));
         zone.broadcast(WorldPackets.playerMove(p), p);
         testLog(p, "Đã tới cạnh " + plantName(plant.templateId) + ".");
      });
   }

   private static String plantName(int templateId) {
      GamePack.Plant template = GameDataCache.plant(templateId);
      return template != null ? template.name() : "cây #" + templateId;
   }

   /** Như {@link #onPlayerZone} nhưng không chặn khi khoá / bị hạ gục (lệnh test). */
   private void onZone(PlayerSession session, BiConsumer<Zone, WorldPlayer> action) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone) {
            action.accept(zone, player);
         }
      });
   }

   // ------------------------------------------------------------------ quái săn bắt (tổ ong, quả dừa)

   // ---- vật phẩm dưới đất (op 30)

   /** Khoảng cách tối đa (pixel, mỗi trục) để nhặt; client tự đi tới ~100 px mới gửi. */
   public static final float PICKUP_REACH_PX = 250f;

   /** Nhặt xong (luồng tick khu): nơi gọi chuyển việc cho vào túi sang luồng session. */
   public interface PickupDone {
      void accept(WorldPlayer player, ItemData item);
   }

   /** Đồ "Tự nhặt vật phẩm" đã gỡ khỏi đất -> cho vào túi ({@code item/GroundItemService} đăng ký lúc khởi động). */
   private volatile PickupDone autoPickupDone;

   public void setAutoPickup(PickupDone done) {
      this.autoPickupDone = done;
   }

   PickupDone autoPickup() {
      return autoPickupDone;
   }

   /**
    * Làm rơi {@code items} quanh (x, y) xuống mặt đất bên dưới (op 30 sub 1 cả khu), {@code ownerId} được ưu tiên nhặt.
    * Luồng tick khu.
    */
   static void dropItems(Zone zone, float x, float y, int ownerId, List<ItemData> items) {
      long now = System.currentTimeMillis();
      MapTemplate map = zone.map().template();
      for (int i = 0; i < items.size(); i++) {
         float dx = map.clampX(x + (i - (items.size() - 1) / 2f) * 40f); // nhiều món: xếp hàng ngang
         zone.drops().add(items.get(i), dx, PlantSpots.groundBelow(map, dx, y), ownerId, now, zone.broadcastAll());
      }
   }

   /**
    * Làm rơi đồ cả khu THẤY nhưng chỉ {@code owner} nhặt được (vd Nanh Hổ răng kiếm bước 13.1), không nhặt thì biến mất.
    * Luồng tick khu.
    */
   static void dropOwnerOnly(Zone zone, float x, float y, WorldPlayer owner, ItemData item) {
      MapTemplate map = zone.map().template();
      float dx = map.clampX(x);
      zone.drops().addOwnerOnly(item, dx, PlantSpots.groundBelow(map, dx, y), owner.id(), System.currentTimeMillis(),
            zone.broadcastAll());
   }

   /**
    * Làm rơi nhiều món cả khu THẤY nhưng chỉ {@code ownerId} nhặt được, nằm {@code lifetimeMs} rồi biến mất (quà boss thế
    * giới cho người gây nhiều sát thương nhất). Luồng tick khu.
    */
   static void dropOwnerOnly(Zone zone, float x, float y, int ownerId, List<ItemData> items, long lifetimeMs) {
      long now = System.currentTimeMillis();
      MapTemplate map = zone.map().template();
      for (int i = 0; i < items.size(); i++) {
         float dx = map.clampX(x + (i - (items.size() - 1) / 2f) * 40f); // nhiều món: xếp hàng ngang
         zone.drops().addOwnerOnly(items.get(i), dx, PlantSpots.groundBelow(map, dx, y), ownerId, now, lifetimeMs,
               zone.broadcastAll());
      }
   }

   /** Làm rơi đồ RIÊNG của {@code owner} (vật phẩm nhiệm vụ) ở mặt đất dưới (x, y) — chỉ họ thấy / nhặt. Luồng tick khu. */
   static void dropPersonal(Zone zone, float x, float y, WorldPlayer owner, ItemData item) {
      MapTemplate map = zone.map().template();
      float dx = map.clampX(x);
      zone.drops().addPersonal(item, dx, PlantSpots.groundBelow(map, dx, y), owner.id(), System.currentTimeMillis(),
            owner::send);
   }

   /**
    * C→S op 30 sub 0: nhặt vật phẩm {@code dropId}. Trên luồng tick khu: còn không, đứng gần không, hết thời gian ưu
    * tiên của người phá chưa -> gỡ khỏi đất, phát op 30 sub 3, gọi {@code done}.
    */
   public void pickUp(PlayerSession session, int dropId, PickupDone done) {
      onPlayerZone(session, (zone, p) -> {
         long now = System.currentTimeMillis();
         ZoneDrops.Drop drop = zone.drops().get(dropId);
         if (drop == null) {
            p.send(Packets.notice("Vật phẩm không còn nữa."));
            return;
         }
         long wait = drop.waitFor(p.id(), now);
         if (wait == Long.MAX_VALUE) {
            p.send(Packets.notice("Vật phẩm nhiệm vụ của người khác."));
            return;
         }
         if (wait > 0) {
            p.send(Packets.notice("Vật phẩm của người khác, " + (wait + 999) / 1000 + " giây nữa mới nhặt được."));
            return;
         }
         if (Math.abs(p.x - drop.x) > PICKUP_REACH_PX || Math.abs(p.y - drop.y) > PICKUP_REACH_PX) {
            p.send(Packets.notice("Hãy lại gần hơn để nhặt."));
            return;
         }
         zone.drops().remove(drop);
         zone.broadcast(WorldPackets.groundItemPicked(p.id(), drop.id).toPacket(), null);
         done.accept(p, drop.item);
      });
   }

   /** Nhặt rồi mà không cho vào túi được (túi đầy / đang giao dịch): thả lại dưới chân, người đó vẫn được ưu tiên. */
   public void dropAtFeet(PlayerSession session, ItemData item) {
      onZone(session, (zone, p) -> dropItems(zone, p.x, p.y + 50, p.id(), List.of(item)));
   }

   /** Lệnh test: tổ ong / quả dừa trong khu + toàn server. */
   public void gatherInfo(PlayerSession session) {
      onZone(session, (zone, p) -> {
         long now = System.currentTimeMillis();
         int mapId = zone.map().template().id();
         StringBuilder sb = new StringBuilder("Map " + mapId + " khu " + zone.id() + ":");
         for (GatherConfig.Entry e : GatherConfig.current().byMob().values()) {
            SpawnQuota q = SpawnQuota.mob(e.mobId());
            sb.append(' ').append(mobName(e.mobId())).append(' ').append(zone.mobs().countGathers(e.mobId()))
                  .append('/').append(e.spots(mapId).isEmpty() ? 0 : e.perZone()).append(" (server ")
                  .append(q.alive()).append(e.total() > 0 ? "/" + e.total() : "").append(", chờ hồi ")
                  .append(q.cooling(now)).append(").");
         }
         for (ZoneMobs.Gather g : zone.mobs().gathers()) {
            sb.append(' ').append(mobName(g.entry.mobId())).append(" HP ").append(g.hp).append('/')
                  .append(g.entry.hp()).append(" ở x=").append(Math.round(g.mob.x())).append('.');
         }
         for (GatherConfig.Entry e : GatherConfig.current().byMob().values()) {
            sb.append(" | ").append(mobName(e.mobId())).append(": ").append(where(SpawnQuota.mob(e.mobId()),
                  e.spots().keySet()));
         }
         testLog(p, sb.toString());
      });
   }

   /** Lệnh test: bỏ thời gian hồi tổ ong / quả dừa toàn server + rải ngay. */
   public void gatherSpawnNow(PlayerSession session) {
      for (GatherConfig.Entry e : GatherConfig.current().byMob().values()) {
         SpawnQuota.mob(e.mobId()).clearCooling();
      }
      runSpawns();
      WorldPlayer player = find(session);
      if (player != null) {
         testLog(player, "Đã bỏ thời gian chờ, tổ ong / quả dừa được rải lại ngẫu nhiên toàn server.");
      }
   }

   /** Lệnh test: dịch chuyển tới cạnh 1 tổ ong / quả dừa trong khu. */
   public void teleportToGather(PlayerSession session) {
      onZone(session, (zone, p) -> {
         List<ZoneMobs.Gather> list = new ArrayList<>();
         zone.mobs().gathers().forEach(list::add);
         if (list.isEmpty()) {
            testLog(p, "Khu này không có tổ ong / quả dừa nào (/ongmoc để mọc).");
            return;
         }
         ZoneMobs.Gather g = list.get(ThreadLocalRandom.current().nextInt(list.size()));
         float x = zone.map().template().clampX(g.mob.x() - 80);
         float y = g.mob.y();
         p.x = x;
         p.y = y;
         p.data().x = x;
         p.data().y = y;
         p.lastMoveAt = System.currentTimeMillis();
         p.invalidateSpawnPacket();
         p.send(WorldPackets.teleportSelf(x, y));
         zone.broadcast(WorldPackets.playerMove(p), p);
         testLog(p, "Đã tới cạnh " + mobName(g.entry.mobId()) + ".");
      });
   }

   // ------------------------------------------------------------------ treo offline (offline/OfflineTrainingService)

   /**
    * Việc bot treo offline cần biết (chụp từ bảng Thiết lập lúc bấm "Luyện tập Offline"): loại quái ({@code templates}
    * rỗng = mọi loại), Né siêu quái ({@code normalOnly}), Không đánh Boss ({@code noBoss}), mục tiêu đang đánh, Tự nhặt,
    * Phạm vi đánh ({@code range} px quanh chỗ bắt đầu treo, 0 = không giới hạn — decompile C3541), tầm X / Y từng kỹ
    * năng sẽ dùng ({@code reachX[i]} / {@code reachY[i]}, theo {@code skill_level} cấp đã học).
    */
   public record OfflineQuery(int[] templates, boolean normalOnly, boolean noBoss, int current, boolean pickup,
         int[] reachX, int[] reachY,
         int range, int spread, boolean spareBoss) {

      /**
       * Luyện tập Offline: không né quái người khác đang đánh. {@code spread} (bot): 0 = không, 1 = hơi né (auto đánh),
       * 2 = né hẳn quái đang có người / bot khác đánh; {@code spareBoss} (bot săn boss): boss dưới 20% HP thì thôi đánh —
       * để người thật kết liễu, bot không cướp quà.
       */
      public OfflineQuery(int[] templates, boolean normalOnly, boolean noBoss, int current, boolean pickup,
            int[] reachX, int[] reachY, int range) {
         this(templates, normalOnly, noBoss, current, pickup, reachX, reachY, range, 0, false);
      }
   }

   /**
    * Trả lời trên luồng tick khu: id quái (-1 = không có / đang đi nhặt) + bit i = kỹ năng thứ i của {@link
    * OfflineQuery#reachX} đang với tới (0 = chưa tới tầm, khu đang chạy tới). Chỉ chuyển việc đi.
    */
   public interface OfflineReply {
      void accept(int mobId, int skillMask);
   }

   /**
    * Bot treo offline hỏi việc tiếp theo: khu tự CHẠY nhân vật tới quái / đồ rơi ({@link Zone#offlineSeek} +
    * {@link OfflineWalker}) như người chơi thật — không dịch chuyển.
    */
   public void offlineSeek(PlayerSession session, OfflineQuery query, OfflineReply reply) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         reply.accept(-1, 0);
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            reply.accept(-1, 0);
            return;
         }
         zone.offlineSeek(player, query, reply);
      });
   }

   /**
    * Kết quả lệnh test (/cay, /caydau, /ong...): ghi LOG server (không dấu, mỗi câu 1 dòng) thay vì thông báo trong game
    * — chỉ để test nội bộ.
    */
   private static void testLog(WorldPlayer p, String text) {
      String who = Log.ascii(p.data().name);
      for (String line : text.split("(?<=\\.) (?=\\S)|\\s*\\|\\s*")) {
         if (!line.isBlank()) {
            Log.info("[test %s] %s", who, Log.ascii(line.trim()));
         }
      }
   }

   private static String mobName(int mobId) {
      MapPack.Mob template = GameDataCache.mob(mobId);
      return template != null ? template.name() : "quái #" + mobId;
   }

   // ------------------------------------------------------------------ cửa dịch chuyển (opcode 11)

   public void useWaypoint(PlayerSession session, int waypointPosition) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         // Client gửi lại opcode 11 mỗi lần chạm cửa -> bỏ qua lặp lại ngay sau khi vừa chuyển map.
         if (player.zone != zone || System.currentTimeMillis() - player.lastTransferAt < WAYPOINT_REPEAT_MS) {
            return;
         }
         MapTemplate from = zone.map().template();
         MapTemplate.Waypoint waypoint = from.waypointAt(waypointPosition);
         if (waypoint == null || !waypoint.contains(player.x, player.y, WAYPOINT_REACH_PX)) {
            Log.debug("Nhan vat #%d dung cua %d o map %d khi khong dung gan cua", player.id(), waypointPosition,
                  from.id());
            return;
         }
         GameMap target = waypoint.targetMapId() == null ? null : maps.get(waypoint.targetMapId());
         if (target == null) {
            player.send(Packets.notice("Khu vực này chưa mở."));
            return;
         }
         if (target.isThunder() || zone.map().isThunder()) {
            player.send(Packets.notice(target.isThunder() ? THUNDER_VIA_NPC : THUNDER_NO_TELEPORT));
            return;
         }
         if (target.isArena() || zone.map().isArena()) {
            player.send(Packets.notice(target.isArena() ? ARENA_VIA_NPC : ARENA_NO_TELEPORT));
            return;
         }
         // Tế đàn: vào qua Tư tế Buraen (altar/AltarService), không khoá theo nhiệm vụ chính tuyến
         if (!target.isAltar() && !target.isLinhMoc()
               && !QuestEvents.canEnter(player.data(), target.template().id())) {
            player.send(Packets.notice(MainQuests.MAP_LOCKED));
            return;
         }
         if (zone.map().isAltar() && target.isAltar()
               && AltarRules.stage(target.template().id()) > AltarRules.stage(from.id()) && !zone.altarCleared()) {
            player.send(Packets.notice(DOOR_LOCKED));
            return;
         }
         // Rừng Linh Mộc: map N+1 chỉ mở khi khu map N của lượt đã dọn xong (quay lại map trước thì luôn được)
         if (zone.linhMoc != null && target.isLinhMoc()
               && LinhMocRules.stage(target.template().id()) > LinhMocRules.stage(from.id()) && !zone.linhMocCleared()) {
            player.send(Packets.notice(LinhMocRules.config().text("doorLocked")));
            return;
         }
         Zone to = reserve(target, player.data());
         if (to == null) {
            player.send(Packets.notice(target.isLinhMoc() ? linhMocUnavailable(player.data()) : ALTAR_OVER));
            return;
         }
         float[] arrival = arrivalPoint(target.template(), from.id(), waypoint);
         transfer(player, zone, target, to, arrival[0], arrival[1]);
      });
   }

   /** Điểm tới ở map đích: toạ độ cấu hình sẵn, hoặc cạnh cửa dẫn ngược về map cũ, hoặc spawn mặc định. */
   private static float[] arrivalPoint(MapTemplate target, int fromMapId, MapTemplate.Waypoint used) {
      if (used.targetX() != null && used.targetY() != null) {
         return new float[] {used.targetX(), used.targetY()};
      }
      MapTemplate.Waypoint back = target.waypointTo(fromMapId);
      if (back == null) {
         return new float[] {target.spawnX(), target.spawnY()};
      }
      boolean doorOnLeftHalf = back.x() + back.w() / 2 < target.width() / 2f;
      float x = doorOnLeftHalf ? back.x() + back.w() + WAYPOINT_ARRIVAL_OFFSET_PX : back.x() - WAYPOINT_ARRIVAL_OFFSET_PX;
      return new float[] {target.clampX(x), target.clampY(back.y() + 10)};
   }

   // ------------------------------------------------------------------ khu (opcode 25)

   // ------------------------------------------------------------------ hộ tống (nhiệm vụ)

   /**
    * Bắt đầu hộ tống thú cưng {@code itemId} (item loại 37 -> {@code char_template_id} ở {@code item_appearance}) trên
    * luồng tick khu. Đang chuyển khu (chưa có khu) thì bỏ — vào khu mới sẽ thấy đang ở bước hộ tống mà không có sinh
    * vật -> thất bại, làm lại bước trước.
    */
   public void startEscort(PlayerSession session, int itemId, List<Integer> maps) {
      WorldPlayer p = online(session);
      Zone zone = p == null ? null : p.zone();
      ItemAppearance look = GameDataCache.itemAppearance(itemId);
      ItemPack.ItemTemplate item = GameDataCache.itemTemplate(itemId);
      if (zone == null || look == null || look.charTemplateId() <= 0
            || GameDataCache.charTemplate(look.charTemplateId()) == null) {
         Log.warn("Khong bat dau ho tong duoc: item %d thieu item_appearance.char_template_id (hoac dang chuyen khu)", itemId);
         return;
      }
      String name = item != null ? item.name() : "";
      zone.submit(() -> {
         if (p.zone() == zone) {
            zone.startEscort(p, look.charTemplateId(), name, maps);
         }
      });
   }

   /** Thôi hộ tống (xong / đổi bước nhiệm vụ). */
   public void stopEscort(PlayerSession session) {
      WorldPlayer p = online(session);
      Zone zone = p == null ? null : p.zone();
      if (zone == null) {
         if (p != null) {
            p.data().escorting = false;
         }
         return;
      }
      zone.submit(() -> {
         if (p.zone() == zone) {
            zone.removeEscort(p);
         }
      });
   }

   /** Cây táo vừa đổi (thu hoạch / nâng cấp): gửi lại trạng thái nếu người chơi đang ở Nông trại của mình. */
   public void refreshAppleTree(PlayerSession session) {
      WorldPlayer p = online(session);
      Zone zone = p == null ? null : p.zone();
      if (zone != null) {
         zone.submit(() -> {
            if (p.zone() == zone && zone.map().template().id() == AppleTreeService.FARM_MAP_ID) {
               zone.showAppleTree(p);
            }
         });
      }
   }

   /** Sub 0: danh sách khu của map đang đứng. */
   public void sendZoneList(PlayerSession session) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone != null && (zone.map().isDungeon() || zone.map().isThunder() || zone.map().isArena())) { // phó bản / Lôi vực / đấu trường: không mở danh sách khu
         player.send(Packets.notice(NO_ZONE_CHANGE));
         return;
      }
      if (zone != null) {
         // Map riêng tư: chỉ có khu của chính mình.
         player.send(WorldPackets.zoneList(zone.map().isPrivate()
               ? List.of(new GameMap.ZoneInfo(zone.id(), zone.occupancy(), zone.maxPlayers(), zone.partyCount()))
               : zone.map().zoneInfos()));
      }
   }

   /** Sub 1: sang khu khác cùng map, giữ nguyên vị trí. */
   public void changeZone(PlayerSession session, int zoneId) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone) {
            return;
         }
         if (zoneId == zone.id()) {
            player.send(Packets.notice("Bạn đang ở khu " + zoneId + "."));
            return;
         }
         if (!checkCooldown(player)) {
            return;
         }
         GameMap map = zone.map();
         if (map.isThunder() || map.isArena()) {
            player.send(Packets.notice(NO_ZONE_CHANGE));
            return;
         }
         if (map.isPrivate()) {
            player.send(Packets.notice(map.isDungeon() ? NO_ZONE_CHANGE : "Đây là khu vực riêng, không thể đổi khu."));
            return;
         }
         Zone target = map.reserveZone(zoneId);
         if (target == null) {
            player.send(Packets.notice(map.hasZone(zoneId) ? "Khu " + zoneId + " đã đầy." : "Khu vực không tồn tại."));
            return;
         }
         transfer(player, zone, map, target, player.x, player.y);
      });
   }

   /**
    * Sub 2: dịch chuyển tới map khác qua bản đồ thế giới (đã trừ Bùa dịch chuyển — {@code item/UsableItems}).
    * Không chuyển được (map khoá, hồi chuyển map, đã rời khu...) -> {@code onRejected} chạy trên luồng khu (hoặc luồng
    * gọi nếu chưa ở khu nào) để hoàn bùa.
    */
   public void teleportToMap(PlayerSession session, int targetMapId, Runnable onRejected) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         onRejected.run();
         return;
      }
      zone.submit(() -> {
         if (!tryTeleport(player, zone, targetMapId)) {
            onRejected.run();
         }
      });
   }

   /**
    * Bảng Boss thế giới "Dịch chuyển" ({@code worldboss/WorldBossList}, đã trừ lam thạch): tới ĐÚNG khu ({@code zoneId})
    * của map {@code mapId} có boss mã {@code key}, đứng cạnh boss. Đọc chỗ boss trên luồng khu boss, rồi kiểm tra + chuyển
    * trên luồng khu người chơi. Không đi được (boss đã chết, khu đầy, đang ở phó bản / chiến trường, bị hạ gục, hồi chuyển
    * map, map khoá nhiệm vụ) -> đã báo lý do + {@code onRejected} (hoàn tiền). Mọi luồng.
    */
   public void teleportToWorldBoss(PlayerSession session, int mapId, int zoneId, int key, Runnable onRejected) {
      WorldPlayer player = find(session);
      GameMap target = maps.get(mapId);
      if (player == null || player.zone == null || target == null) {
         onRejected.run();
         return;
      }
      worldBossSpot(mapId, zoneId, key).whenComplete((spot, error) -> {
         Zone zone = player.zone;
         if (zone == null) {
            onRejected.run();
            return;
         }
         zone.submit(() -> {
            if (!tryTeleportToBoss(player, zone, target, zoneId, error == null ? spot : null)) {
               onRejected.run();
            }
         });
      });
   }

   /** Luồng khu người chơi. @return false nếu không chuyển (đã báo lý do) */
   private boolean tryTeleportToBoss(WorldPlayer player, Zone zone, GameMap target, int zoneId, float[] spot) {
      if (player.zone != zone) {
         return false;
      }
      if (spot == null) {
         player.send(Packets.notice("Boss đã bị hạ hoặc đã rời đi."));
         return false;
      }
      if (player.data().isDead()) {
         player.send(Packets.notice("Đã bị hạ gục, không thể dịch chuyển."));
         return false;
      }
      if (adminTeleportBlocked(zone.map().template().id())
            || adminTeleportBlocked(target.template().id())) {
         player.send(Packets.notice("Không thể dịch chuyển khi đang ở phó bản, chiến trường hoặc đài đấu."));
         return false;
      }
      if (!QuestEvents.canEnter(player.data(), target.template().id())) {
         player.send(Packets.notice(MainQuests.MAP_LOCKED));
         return false;
      }
      float side = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
      float x = target.template().clampX(spot[0] + side * 150);
      float y = spot[1] + 5;
      if (zone.map() == target && zone.id() == zoneId) { // đã ở đúng khu: chỉ dời chỗ
         player.x = x;
         player.y = y;
         player.data().x = x;
         player.data().y = y;
         player.lastMoveAt = System.currentTimeMillis();
         player.invalidateSpawnPacket();
         player.send(WorldPackets.teleportSelf(x, y));
         zone.broadcast(WorldPackets.playerMove(player), player);
         return true;
      }
      if (!checkCooldown(player)) {
         return false;
      }
      Zone to = target.reserveZone(zoneId);
      if (to == null) {
         player.send(Packets.notice("Khu vực có boss đã đầy, thử lại sau."));
         return false;
      }
      transfer(player, zone, target, to, x, y);
      return true;
   }

   /** Map không cho admin dịch chuyển tới / đi khỏi trực tiếp (phó bản, chiến trường, đài đấu — có lượt / trạng thái riêng). */
   public boolean adminTeleportBlocked(int mapId) {
      GameMap m = maps.get(mapId);
      return m == null || m.isAltar() || m.isLinhMoc() || m.isThunder() || m.isArena();
   }

   /**
    * Admin panel dịch chuyển người đang online tới map {@code mapId}, chỗ ({@code x}, {@code y}) — null = điểm xuất hiện của
    * map. Bỏ qua hồi chuyển map / khoá nhiệm vụ. Không đụng phó bản / chiến trường. Future = câu báo lỗi, null = đã chuyển.
    */
   public java.util.concurrent.CompletableFuture<String> adminTeleport(PlayerSession session, int mapId, Float x, Float y) {
      java.util.concurrent.CompletableFuture<String> out = new java.util.concurrent.CompletableFuture<>();
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      GameMap target = maps.get(mapId);
      if (zone == null) {
         out.complete("Nhân vật chưa ở trong khu nào.");
      } else if (target == null) {
         out.complete("Không có map " + mapId + ".");
      } else if (adminTeleportBlocked(mapId) || adminTeleportBlocked(zone.map().template().id())) {
         out.complete("Không dịch chuyển vào / ra phó bản, chiến trường, đài đấu.");
      } else {
         zone.submit(() -> {
            if (player.zone != zone) {
               out.complete("Nhân vật vừa đổi khu, thử lại.");
               return;
            }
            float tx = x != null ? x : target.template().spawnX();
            float ty = y != null ? y : target.template().spawnY();
            transfer(player, zone, target, target.reserveFor(player.id()), tx, ty);
            out.complete(null);
         });
      }
      return out;
   }

   /** Luồng khu: kiểm tra rồi chuyển map. @return false nếu không chuyển (đã báo lý do nếu cần) */
   private boolean tryTeleport(WorldPlayer player, Zone zone, int targetMapId) {
      if (player.zone != zone) {
         return false;
      }
      GameMap target = maps.get(targetMapId);
      if (target == null) {
         player.send(Packets.notice("Khu vực này chưa mở."));
         return false;
      }
      if (target == zone.map()) {
         player.send(Packets.notice("Bạn đang ở " + target.template().name() + "."));
         return false;
      }
      if (zone.map().isThunder()) {
         player.send(Packets.notice(THUNDER_NO_TELEPORT));
         return false;
      }
      if (target.isThunder()) {
         player.send(Packets.notice(THUNDER_VIA_NPC));
         return false;
      }
      if (zone.map().isArena()) {
         player.send(Packets.notice(ARENA_NO_TELEPORT));
         return false;
      }
      if (target.isArena()) {
         player.send(Packets.notice(ARENA_VIA_NPC));
         return false;
      }
      if (!QuestEvents.canEnter(player.data(), targetMapId)) {
         player.send(Packets.notice(MainQuests.MAP_LOCKED));
         return false;
      }
      if (target.isAltar()) {
         player.send(Packets.notice("Hãy gặp Tư tế Buraen ở Làng ánh sáng Eldarah để vào Tế đàn Thiên tích."));
         return false;
      }
      if (target.isLinhMoc()) {
         player.send(Packets.notice(LinhMocRules.config().text("viaNpc")));
         return false;
      }
      if (!checkCooldown(player)) {
         return false;
      }
      transfer(player, zone, target, reserveNearParty(target, player.data()), target.template().spawnX(),
            target.template().spawnY());
      return true;
   }

   private static boolean checkCooldown(WorldPlayer player) {
      long now = System.currentTimeMillis();
      long waitMs = player.lastTransferAt + TRANSFER_COOLDOWN_MS - now;
      if (waitMs > 0) {
         player.send(Packets.notice("Vui lòng đợi " + ((waitMs + 999) / 1000) + " giây."));
         return false;
      }
      return true; // lastTransferAt chỉ cập nhật khi chuyển thật (transfer)
   }

   /**
    * Chạy trên luồng khu cũ: rời khu cũ rồi xếp lệnh vào khu mới (chỗ đã giữ sẵn trong {@code to}).
    * Nếu người chơi vừa thoát, lệnh join ở khu mới tự trả lại chỗ.
    */
   private static void transfer(WorldPlayer player, Zone from, GameMap toMap, Zone to, float x, float y) {
      player.lastTransferAt = System.currentTimeMillis();
      from.leave(player);
      to.submit(() -> to.join(player, toMap.template().clampX(x), toMap.template().clampY(y)));
   }

   // ------------------------------------------------------------------ Tế đàn Thiên tích

   static final String ALTAR_OVER = "Lượt Tế đàn Thiên tích đã kết thúc.";
   /** Bấm Khu vực / đổi khu trong phó bản (Tế đàn, Rừng Linh Mộc). */
   static final String NO_ZONE_CHANGE = "Không thể đổi khu ở đây";
   /** Cửa sang map sau khi map đang đứng chưa diệt hết (Tế đàn; Rừng Linh Mộc dùng chữ cấu hình {@code doorLocked}). */
   static final String DOOR_LOCKED = "Chưa đủ điều kiện qua map này";

   /**
    * Vào Tế đàn ở map {@code mapId} (34-40) của lượt đang chạy ({@code PlayerData.altarRun} — {@code AltarService} đã bắt
    * đầu / kiểm tra lượt), đứng ở điểm xuất hiện của map. Đang ở chính map đó thì thôi.
    */
   public void enterAltar(PlayerSession session, int mapId) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      GameMap target = maps.get(mapId);
      if (zone == null || target == null || !target.isAltar()) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || zone.map() == target) {
            return;
         }
         if (player.data().isDead()) {
            player.send(Packets.notice("Đã bị hạ gục, không thể vào Tế đàn."));
            return;
         }
         Zone to = reserve(target, player.data());
         if (to == null) {
            player.send(Packets.notice(ALTAR_OVER));
            return;
         }
         transfer(player, zone, target, to, target.template().spawnX(), target.template().spawnY());
      });
   }

   /**
    * Rời Tế đàn về điểm xuất hiện của Làng (nút "Thoát" op 25 sub 3, Buraen "Về làng", hết lượt). Không ở map Tế đàn thì
    * thôi. Lượt vẫn chạy (còn giờ thì vào lại được). {@code notice} (có thể null) gửi sau khi tới làng.
    */
   public void leaveAltar(PlayerSession session, String notice) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || !zone.map().isAltar()) {
            return;
         }
         boolean dead = player.data().isDead();
         if (dead) {
            revive(player, 100); // bị hạ gục lúc hết lượt: về làng như "Về làng"
         }
         GameMap village = maps.get(startMapId);
         Zone to = village.reserveFor(player.id());
         transfer(player, zone, village, to, village.template().spawnX(), village.template().spawnY());
         to.submit(() -> {
            if (player.zone == to) {
               if (dead) {
                  broadcastRevive(to, player);
               }
               if (notice != null) {
                  player.send(Packets.notice(notice));
               }
            }
         });
      });
   }

   /** Lệnh test: gửi lại đồng hồ Tế đàn (giờ kết thúc vừa đổi). */
   public void refreshAltarTimer(PlayerSession session) {
      onZone(session, (zone, p) -> {
         if (zone.map().isAltar()) {
            Zone.sendAltarTimer(p);
         }
      });
   }

   /** Lệnh test: hạ mọi quái của map Tế đàn đang đứng, chừa {@code keep} con (thử cửa khoá / mở, lên cấp rương). */
   public void testKillAltarMobs(PlayerSession session, int keep) {
      onZone(session, (zone, p) -> {
         if (!zone.map().isAltar()) {
            p.send(Packets.notice("Chỉ dùng trong Tế đàn Thiên tích."));
            return;
         }
         ZoneCombat.testKillAll(zone, keep);
         p.send(Packets.notice("Còn " + zone.altarAlive() + " quái / boss."));
      });
   }

   /** Lượt Tế đàn của {@code playerId} kết thúc: trả số khu, các khu Tế đàn của họ trống thì đóng. */
   public void endAltarRun(int playerId) {
      altarZones.release(playerId);
      for (GameMap map : maps.values()) {
         if (map.isAltar()) {
            map.releaseAltar(playerId);
         }
      }
   }

   // ------------------------------------------------------------------ Lôi vực Sấm sét

   static final String THUNDER_VIA_NPC = "Hãy gặp Tư tế Buraen ở Làng ánh sáng Eldarah để vào Lôi vực Sấm sét.";
   static final String THUNDER_NO_TELEPORT = "Không thể dịch chuyển ở Lôi vực Sấm sét, hãy gặp Tư tế để về làng.";
   /** Tới Lôi vực: đứng cách Tư tế bấy nhiêu px (phía trước mặt). */
   static final float THUNDER_ARRIVAL_OFFSET_PX = 200f;

   /**
    * Vào map Lôi vực {@code mapId} (lượt đã trừ ở {@code thunder/ThunderService}): khu số nhỏ nhất (map 1 khu, không giới
    * hạn người), đứng trước mặt Tư tế Buraen. {@code onFail} chạy khi không vào được (đã bị hạ gục / đang chuyển khu /
    * đã ở đó) — để dịch vụ hoàn lượt.
    */
   public void enterThunder(PlayerSession session, int mapId, Runnable onFail) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      GameMap target = maps.get(mapId);
      if (zone == null || target == null || !target.isThunder()) {
         onFail.run();
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || zone.map().isThunder() || player.data().isDead()) {
            onFail.run();
            return;
         }
         float[] at = thunderArrival(target.template());
         transfer(player, zone, target, target.reserveFor(player.id()), at[0], at[1]);
      });
   }

   /** Chỗ đứng lúc tới Lôi vực: trước mặt Tư tế Buraen (NPC 17), không có thì điểm xuất hiện của map. */
   static float[] thunderArrival(MapTemplate t) {
      if (t.npcs() != null) {
         for (MapTemplate.NpcSpawn npc : t.npcs()) {
            if (npc.id() == com.tlnt.server.altar.AltarService.NPC_BURAEN) {
               float x = npc.x() + (npc.flipX() ? THUNDER_ARRIVAL_OFFSET_PX : -THUNDER_ARRIVAL_OFFSET_PX);
               return new float[] {t.clampX(x), t.clampY(npc.y())};
            }
         }
      }
      return new float[] {t.spawnX(), t.spawnY()};
   }

   /**
    * Rời Lôi vực về điểm xuất hiện của Làng (Tư tế "Về Làng", nút Thoát, hết giờ). Không ở map Lôi vực thì thôi.
    * {@code notice} (có thể null) gửi sau khi tới làng. Lượt kết thúc qua {@link ThunderHooks#left} ({@code Zone.join}).
    */
   public void leaveThunder(PlayerSession session, String notice) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone && zone.map().isThunder()) {
            exitThunder(zone, player, notice);
         }
      });
   }

   /** Luồng khu Lôi vực: về làng (bị hạ gục thì hồi sinh đầy HP / MP trước), {@code notice} báo sau khi tới. */
   void exitThunder(Zone zone, WorldPlayer player, String notice) {
      boolean dead = player.data().isDead();
      if (dead) {
         revive(player, 100);
      }
      GameMap village = maps.get(startMapId);
      Zone to = village.reserveFor(player.id());
      transfer(player, zone, village, to, village.template().spawnX(), village.template().spawnY());
      to.submit(() -> {
         if (player.zone == to) {
            if (dead) {
               broadcastRevive(to, player);
            }
            if (notice != null) {
               player.send(Packets.notice(notice));
            }
         }
      });
   }

   /** Lôi vực: hồi sinh TẠI CHỖ đầy HP / MP (tự động sau 5 giây). Luồng khu. */
   static void reviveInPlace(Zone zone, WorldPlayer player) {
      revive(player, 100);
      broadcastRevive(zone, player);
   }

   /** Lệnh test: gửi lại đồng hồ Lôi vực (giờ kết thúc vừa đổi). */
   public void refreshThunderTimer(PlayerSession session) {
      onZone(session, (zone, p) -> {
         if (zone.map().isThunder()) {
            Zone.sendThunderTimer(p);
         }
      });
   }

   // ------------------------------------------------------------------ Giải đấu Bộ lạc (map 22)

   static final String ARENA_VIA_NPC = "Hãy gặp Tư tế Buraen ở Làng ánh sáng Eldarah để tham gia Giải đấu Bộ lạc.";
   static final String ARENA_NO_TELEPORT = "Không thể dịch chuyển ở đấu trường, hãy gặp Tư tế để về làng.";
   /** Làng ánh sáng Eldarah (nơi tập trung / chờ vòng đấu). */
   public static final int VILLAGE_MAP_ID = 2;

   /**
    * Đưa người chơi {@code playerId} vào khu đấu {@code arena} ở chỗ {@code x, y}, hồi đầy HP / MP (kể cả đang bị hạ gục).
    * {@code fromVillageOnly}: chỉ khi đang đứng ở Làng (giờ thi đấu). {@code onFail} chạy (luồng khu / luồng gọi) khi không
    * vào được (offline / đang chuyển khu / không ở làng / khu đấu đầy hoặc đã chốt).
    */
   public void enterArena(int playerId, Arena arena, float x, float y, boolean fromVillageOnly, Runnable onFail) {
      WorldPlayer player = online(playerId);
      Zone zone = player == null ? null : player.zone;
      GameMap target = maps.get(Arena.MAP_ID);
      if (zone == null || target == null) {
         onFail.run();
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || player.removed || zone.arena == arena
               || fromVillageOnly && zone.map().template().id() != VILLAGE_MAP_ID) {
            onFail.run();
            return;
         }
         Zone to = target.reserveArena(arena);
         if (to == null) {
            onFail.run();
            return;
         }
         if (player.data().isDead()) {
            revive(player, 100);
            broadcastRevive(zone, player);
         } else {
            healFull(player); // giải đấu: vào trận đầy HP / MP (gói xuất hiện ở khu mới mang HP mới)
         }
         transfer(player, zone, target, to, x, y);
         to.submit(() -> { // xếp SAU lệnh vào khu: thanh HP / MP của chính mình (client giữ nhân vật qua đổi map)
            if (player.zone == to) {
               PlayerData d = player.data();
               player.send(ItemPackets.hpMp(player.id(), d.hp, d.stats.maxHp, d.mp, d.stats.maxMp));
            }
         });
      });
   }

   /** Đưa người chơi đang ở khu đấu về Làng (Tư tế "Về làng", nút Thoát, hộp Kiệt sức "Về làng"). */
   public void leaveArena(PlayerSession session, String notice) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone && zone.arena != null) {
            exitArena(zone, player, notice);
         }
      });
   }

   /** Như trên theo id (giải chốt trận / hết vòng loại). Không còn ở khu đấu {@code arena} thì thôi. */
   public void leaveArena(int playerId, Arena arena, String notice) {
      WorldPlayer player = online(playerId);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone == zone && zone.arena == arena) {
            exitArena(zone, player, notice);
         }
      });
   }

   /** Luồng khu đấu: về điểm xuất hiện của Làng, hồi đầy HP / MP; {@code notice} (có thể null) báo sau khi tới. */
   void exitArena(Zone zone, WorldPlayer player, String notice) {
      boolean dead = player.data().isDead();
      if (dead) {
         revive(player, 100);
      } else {
         healFull(player);
      }
      GameMap village = maps.get(startMapId);
      Zone to = village.reserveFor(player.id());
      transfer(player, zone, village, to, village.template().spawnX(), village.template().spawnY());
      to.submit(() -> {
         if (player.zone == to) {
            if (dead) {
               broadcastRevive(to, player);
            } else {
               PlayerData d = player.data();
               to.broadcast(ItemPackets.hpMp(player.id(), d.hp, d.stats.maxHp, d.mp, d.stats.maxMp).toPacket(), null);
            }
            if (notice != null) {
               player.send(Packets.notice(notice));
            }
         }
      });
   }

   /**
    * Bật / tắt cho đánh ở khu đấu: cờ PK mọi người trong khu (chờ = Hoà bình, đánh = cờ của khu đấu) + đồng hồ op 67
    * {@code timerText} từ {@code timerStart} tới {@code timerEnd} (null = gỡ đồng hồ). Khu chưa mở thì chỉ đặt cờ (người
    * vào sau tự nhận đúng trạng thái).
    */
   public void arenaPhase(Arena arena, boolean fighting, String timerText, long timerStart, long timerEnd) {
      arena.fighting = fighting;
      arena.timerText = timerText;
      arena.timerStart = timerStart;
      arena.timerEnd = timerEnd;
      GameMap map = maps.get(Arena.MAP_ID);
      Zone zone = map == null ? null : map.arenaZone(arena.id);
      if (zone != null) {
         zone.submit(() -> {
            zone.applyArenaFaction();
            for (WorldPlayer p : zone.players()) {
               Zone.sendArenaTimer(p, arena);
            }
         });
      }
   }

   /**
    * Chốt khu đấu: không ai vào thêm được, ai còn trong khu về Làng (đầy HP / MP, {@code notice} báo sau khi tới); trống là
    * đóng khu.
    */
   public void closeArena(Arena arena, String notice) {
      GameMap map = maps.get(Arena.MAP_ID);
      if (map == null) {
         return;
      }
      map.releaseArena(arena);
      Zone zone = map.arenaZone(arena.id);
      if (zone != null) {
         zone.submit(() -> {
            for (WorldPlayer p : new ArrayList<>(zone.players())) {
               exitArena(zone, p, notice);
            }
         });
      }
   }

   /** Đầy HP / MP cho người còn sống (không tính là hồi sinh: không tốn lượt Bùa linh mộc). Luồng khu. */
   private static void healFull(WorldPlayer player) {
      PlayerData d = player.data();
      if (d.stats != null) {
         d.setHpMp(d.stats.maxHp, d.stats.maxMp);
         player.invalidateSpawnPacket();
      }
   }

   /** Người chơi đang online và đứng ở Làng ánh sáng (có mặt lúc vòng đấu bắt đầu). Mọi luồng (đọc gần đúng). */
   public boolean inVillage(int playerId) {
      WorldPlayer p = online(playerId);
      Zone zone = p == null ? null : p.zone;
      return zone != null && zone.map().template().id() == VILLAGE_MAP_ID;
   }

   /** Người chơi đang ở khu đấu {@code arena}. Mọi luồng (đọc gần đúng). */
   public boolean inArena(int playerId, Arena arena) {
      WorldPlayer p = online(playerId);
      Zone zone = p == null ? null : p.zone;
      return zone != null && zone.arena == arena;
   }

   // ------------------------------------------------------------------ Rừng Linh Mộc

   /**
    * Vào map đầu của lượt Rừng Linh Mộc {@code run} (đứng ở điểm xuất hiện). {@code LinhMocService} đã kiểm tra quyền
    * (người chơi trong {@code memberIds}); ở đây kiểm lại lượt còn chạy + khu còn chỗ.
    */
   public void enterLinhMoc(PlayerSession session, LinhMocRun run) {
      enterLinhMoc(session, run, LinhMocRules.firstMap());
   }

   /** Như trên, vào map {@code mapId} của lượt (lệnh test nhảy map). Đang ở chính khu đó thì thôi. */
   public void enterLinhMoc(PlayerSession session, LinhMocRun run, int mapId) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      GameMap target = maps.get(mapId);
      if (zone == null || target == null || !target.isLinhMoc()) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || zone.map() == target && zone.linhMoc == run) {
            return;
         }
         if (player.data().isDead()) {
            player.send(Packets.notice(LinhMocRules.config().text("dead")));
            return;
         }
         if (!run.active(System.currentTimeMillis()) || !run.memberIds.contains(player.id())) {
            player.send(Packets.notice(LinhMocRules.config().text("over")));
            return;
         }
         Zone to = target.reserveLinhMoc(run);
         if (to == null) {
            player.send(Packets.notice(LinhMocRules.config().text("full")));
            return;
         }
         transfer(player, zone, target, to, target.template().spawnX(), target.template().spawnY());
      });
   }

   /**
    * Rời Rừng Linh Mộc về cạnh Bác Rano (nút "Thoát" op 25 sub 3, hết lượt / thành công). Không ở khu Rừng Linh Mộc —
    * hoặc {@code run} khác null mà người chơi đang ở khu của lượt khác — thì thôi. Bị hạ gục thì hồi sinh đầy.
    * {@code notice} (có thể null) gửi sau khi tới nơi.
    */
   public void leaveLinhMoc(PlayerSession session, String notice, LinhMocRun run) {
      WorldPlayer player = find(session);
      Zone zone = player == null ? null : player.zone;
      if (zone == null) {
         return;
      }
      zone.submit(() -> {
         if (player.zone != zone || zone.linhMoc == null || run != null && zone.linhMoc != run) {
            return;
         }
         boolean dead = player.data().isDead();
         if (dead) {
            revive(player, 100);
         }
         GameMap hub = maps.get(LinhMocRules.config().hubMapId());
         if (hub == null) {
            hub = maps.get(startMapId);
         }
         float[] at = linhMocHub(hub);
         Zone to = hub.reserveFor(player.id());
         transfer(player, zone, hub, to, at[0], at[1]);
         to.submit(() -> {
            if (player.zone == to) {
               if (dead) {
                  broadcastRevive(to, player);
               }
               if (notice != null) {
                  player.send(Packets.notice(notice));
               }
            }
         });
      });
   }

   /** Lượt Rừng Linh Mộc kết thúc: khu của lượt ở mọi map hết được giữ (trống 60 s thì đóng). */
   public void endLinhMocRun(LinhMocRun run) {
      for (GameMap map : maps.values()) {
         if (map.isLinhMoc()) {
            map.releaseLinhMoc(run.id);
         }
      }
   }

   /** Gửi lại đồng hồ Rừng Linh Mộc (giờ kết thúc vừa đổi: thành công -> đếm 10 s về, lệnh test) + giữ khu theo giờ mới. */
   public void refreshLinhMocTimer(PlayerSession session) {
      onZone(session, (zone, p) -> {
         if (zone.linhMoc != null) {
            zone.keepUntil = zone.linhMoc.end;
            Zone.sendLinhMocTimer(p, zone.linhMoc);
         }
      });
   }

   /** Lệnh test: hạ mọi quái khu Rừng Linh Mộc đang đứng, chừa {@code keep} con. */
   /**
    * Lệnh test {@code /tinhanh} / {@code /thulinh} / {@code /quaithuong}: con quái thường còn sống GẦN người chơi nhất trong
    * khu thành biến thể {@code variant} ngay (HP đầy; op 20 sub 2 cả khu — client cập nhật chỉ số + hiệu ứng xuất hiện).
    * Bỏ qua giới hạn 1 tinh anh + 1 thủ lĩnh mỗi loại.
    */
   public void testSetMobVariant(PlayerSession session, int variant) {
      onZone(session, (zone, p) -> {
         ZoneMobs mobs = zone.mobs();
         String[] names = {"quái thường", "Tinh anh", "Thủ lĩnh"};
         int best = -1;
         float bestD = Float.MAX_VALUE;
         for (int id = 0; id < mobs.size(); id++) {
            MapTemplate.MobInstance mob = mobs.alive(id);
            if (mob == null) {
               continue;
            }
            float d = Math.abs(mob.x() - p.x) + Math.abs(mob.y() - p.y);
            if (d < bestD && (variant == com.tlnt.server.combat.MobRules.VARIANT_NORMAL
                  ? mob.spawn().variant() != variant : mob.spawn().variant() == 0)) {
               best = id;
               bestD = d;
            }
         }
         MapTemplate.MobInstance changed = best < 0 ? null : mobs.testSetVariant(best, variant);
         if (changed == null) {
            p.send(Packets.notice("Không có quái gần đây đổi được thành " + names[variant]
                  + " (quái phó bản / người rơm / boss không có biến thể)."));
            return;
         }
         zone.broadcast(WorldPackets.mobRespawn(changed).toPacket(), null);
         p.send(Packets.notice("Đã biến " + mobName(changed.spawn().id()) + " (#" + best + ") thành " + names[variant]
               + ", HP " + changed.spawn().hp() + "."));
      });
   }

   public void testKillLinhMocMobs(PlayerSession session, int keep) {
      onZone(session, (zone, p) -> {
         if (zone.linhMoc == null) {
            p.send(Packets.notice("Chỉ dùng trong Rừng Linh Mộc."));
            return;
         }
         ZoneCombat.testKillAll(zone, keep);
         p.send(Packets.notice("Còn " + zone.altarAlive() + " quái / boss."));
      });
   }

   /** Chỗ đứng khi ra khỏi Rừng Linh Mộc: cạnh NPC Bác Rano ở {@code hub} (không có thì điểm xuất hiện). */
   private static float[] linhMocHub(GameMap hub) {
      MapTemplate t = hub.template();
      int npcId = LinhMocRules.config().npcId();
      if (t.npcs() != null) {
         for (MapTemplate.NpcSpawn npc : t.npcs()) {
            if (npc.id() == npcId) {
               return new float[] {t.clampX(npc.x()), t.clampY(npc.y())};
            }
         }
      }
      return new float[] {t.spawnX(), t.spawnY()};
   }

   /** Vì sao không vào được map Rừng Linh Mộc (không có lượt / lượt hết vs khu đầy). */
   private String linhMocUnavailable(PlayerData d) {
      LinhMocHooks hooks = linhMocHooks;
      LinhMocConfig c = LinhMocRules.config();
      return hooks != null && hooks.activeRun(d.getId()) != null ? c.text("full") : c.text("over");
   }

   // ------------------------------------------------------------------ tắt server

   public void shutdown() {
      engine.shutdown();
      for (GameMap map : maps.values()) {
         map.shutdown();
      }
   }
}
