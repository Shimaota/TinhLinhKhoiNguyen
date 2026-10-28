package com.tlnt.server.worldboss;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.FoxThief;
import com.tlnt.server.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mini boss Tiểu Tinh linh Cáo: cả server luôn {@link #COUNT} con, mỗi khu tối đa 1 (khu đã có boss người chơi giả thì
 * thử khu khác — {@code World.placeWorldBoss}), ở map ngẫu nhiên trừ phó bản / map riêng / Nông trại / map chưa mở theo
 * nhiệm vụ. Không thông báo khi xuất hiện / chết (chỉ báo thế giới 1 lần khi 1 con trộm đủ 1 triệu — {@link FoxThief}).
 * Chết -> {@link #RESPAWN_MS} sau con mới ở map khác; đổi map (bị phát hiện quá lâu / đủ 1 triệu) -> mọc ngay ở map
 * khác, cùng trạng thái, đầy HP. Quà = vàng khoá đã trộm chia {@link FoxThief#GOLD_PILES} đống.
 *
 * <p>Luồng: {@link #tick} (scheduler mỗi giây) + sự kiện từ luồng tick khu (listener) — trạng thái dưới khoá {@code this}.
 * Chỉ RAM (tắt máy chủ = vàng đã trộm mất theo).
 */
public final class FoxService {

   public static final int COUNT = 5;
   static final long FIRST_MS = 60_000;
   static final long RESPAWN_MS = 5 * 60_000;
   static final long RETRY_MS = 10_000;
   /** Mã boss (lệnh /boss chet N): 1000 + số thứ tự. */
   static final int KEY_BASE = 1000;
   static final String NAME = "Tiểu Tinh linh Cáo";

   /** Theo bản dump game gốc: char_template 12 (skeletons/mobs/Spirit_Fox), cấp 100, 1000 HP, Né 1000, tốc 950. */
   static final AltarRules.BossSpec SPEC = new AltarRules.BossSpec(0, 0, NAME, 12, 1, 100, 1_000, 1_000, 5,
         new String[0], 100, 100, 180, 0, 0, 1_000, 0, 0, new int[0], null, List.of(), 950, null);

   private final World world;
   private final Slot[] slots = new Slot[COUNT];
   /** Người đứng im khắp server {mapId, zoneId, playerId} — quét mỗi {@link #SCAN_MS}. Dưới khoá this. */
   private List<int[]> candidates = List.of();
   private long nextScanAt;
   static final long SCAN_MS = 5_000;

   private static volatile FoxService instance;

   static final class Slot {
      final int index;
      FoxThief thief;
      boolean alive;
      boolean spawning;
      long nextSpawnAt;
      int mapId;
      String mapName = "";
      int zoneId;

      Slot(int index) {
         this.index = index;
         this.thief = new FoxThief(index);
      }
   }

   public FoxService(World world) {
      this.world = world;
      long now = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
         slots[i] = new Slot(i);
         slots[i].nextSpawnAt = now + FIRST_MS + i * 2_000L;
      }
      instance = this;
   }

   /** Scheduler mỗi giây: mọc các con đã tới giờ. */
   public synchronized void tick() {
      long now = System.currentTimeMillis();
      for (Slot s : slots) {
         if (!s.alive && !s.spawning && now >= s.nextSpawnAt) {
            spawn(s, -1);
         }
      }
      if (now >= nextScanAt) { // người đứng im khắp server + hỏi thăm từng con (rảnh lâu thì sang khu có mục tiêu)
         nextScanAt = now + SCAN_MS;
         candidates = world.foxCandidates(new java.util.HashSet<>(spawnMaps()), now);
         for (Slot s : slots) {
            if (s.alive) {
               world.pokeFox(s.mapId, s.zoneId, KEY_BASE + s.index);
            }
         }
      }
   }

   /**
    * Khu / map khác có người đứng im còn vàng khoá cho con {@code s} (đang rảnh ở khu fromMap / fromZone): không trùng khu
    * con khác đang ở, người đó chưa bị con này trộm đủ 100k; random 1 chỗ. null = không có. Dưới khoá this.
    */
   private int[] pickTarget(Slot s, int fromMapId, int fromZoneId) {
      List<int[]> options = new ArrayList<>();
      for (int[] c : candidates) {
         if (c[0] == fromMapId && c[1] == fromZoneId || s.thief.done(c[2]) || occupied(c[0], c[1])) {
            continue;
         }
         options.add(c);
      }
      return options.isEmpty() ? null : options.get(ThreadLocalRandom.current().nextInt(options.size()));
   }

   /** Khu đã có con cáo khác (đang đứng hoặc đang mọc tới). */
   private boolean occupied(int mapId, int zoneId) {
      for (Slot o : slots) {
         if ((o.alive || o.spawning) && o.mapId == mapId && o.zoneId == zoneId) {
            return true;
         }
      }
      return false;
   }

   /** Map mọc được: map thường (không phó bản / riêng), có trong chuỗi nhiệm vụ, không phải Nông trại. */
   List<Integer> spawnMaps() {
      List<Integer> ids = new ArrayList<>();
      for (MapTemplate m : world.fieldMaps()) {
         if (MainQuests.listed(m.id()) && !MainQuests.isFarm(m.id())) {
            ids.add(m.id());
         }
      }
      return ids;
   }

   /** Gửi yêu cầu mọc con {@code s} ở 1 map ngẫu nhiên khác {@code excludeMapId}. Dưới khoá this. */
   private void spawn(Slot s, int excludeMapId) {
      List<Integer> maps = spawnMaps();
      if (maps.size() > 1) {
         maps.remove(Integer.valueOf(excludeMapId));
      }
      if (maps.isEmpty()) {
         s.nextSpawnAt = System.currentTimeMillis() + RETRY_MS;
         return;
      }
      int mapId = maps.get(ThreadLocalRandom.current().nextInt(maps.size()));
      s.spawning = true;
      world.spawnWorldBoss(mapId, request(s));
   }

   /** Yêu cầu mọc con {@code s} (cùng trạng thái trộm). */
   private World.WorldBossSpawn request(Slot s) {
      FoxThief thief = s.thief;
      return new World.WorldBossSpawn(KEY_BASE + s.index, NAME, 0, SPEC.level(), SPEC.hp(), 0, 0, SPEC, 0, 0, 0,
            () -> loot(thief), new Listener(s, thief), thief);
   }

   /** Vàng khoá đã trộm, chia đống. */
   static List<ItemData> loot(FoxThief thief) {
      List<ItemData> items = new ArrayList<>();
      for (long amount : thief.piles()) {
         ItemData gold = ItemData.of(1, (int) Math.min(Integer.MAX_VALUE, amount));
         gold.locked = true;
         items.add(gold);
      }
      return items;
   }

   /** Sự kiện của 1 con (luồng tick khu). Lượt mọc cũ (con đã chết / thay) thì bỏ qua. */
   private final class Listener implements World.WorldBossListener {
      private final Slot slot;
      private final FoxThief thief;

      Listener(Slot slot, FoxThief thief) {
         this.slot = slot;
         this.thief = thief;
      }

      @Override
      public void spawned(int mapId, String mapName, int zoneId) {
         synchronized (FoxService.this) {
            if (slot.thief != thief) {
               return;
            }
            slot.alive = true;
            slot.spawning = false;
            slot.mapId = mapId;
            slot.mapName = mapName;
            slot.zoneId = zoneId;
         }
      }

      @Override
      public void failed() {
         synchronized (FoxService.this) {
            if (slot.thief != thief) {
               return;
            }
            slot.spawning = false;
            slot.nextSpawnAt = System.currentTimeMillis() + RETRY_MS; // thử map khác
         }
      }

      @Override
      public void killed(int killerId, String killerName) {
         synchronized (FoxService.this) {
            if (slot.thief != thief) {
               return;
            }
            Log.debug("Cao #%d bi ha (nguoi ket lieu %d), roi %d vang khoa", slot.index, killerId, thief.total());
            slot.alive = false;
            slot.spawning = false;
            slot.thief = new FoxThief(slot.index); // con mới: chưa trộm gì
            slot.nextSpawnAt = System.currentTimeMillis() + RESPAWN_MS;
         }
      }

      @Override
      public int[] seekTarget(int fromMapId, int fromZoneId) {
         synchronized (FoxService.this) {
            return slot.thief != thief ? null : pickTarget(slot, fromMapId, fromZoneId);
         }
      }

      @Override
      public void relocateTo(int mapId, int zoneId) {
         synchronized (FoxService.this) {
            if (slot.thief != thief) {
               return;
            }
            Log.debug("Cao #%d sang map %d khu %d tim muc tieu", slot.index, mapId, zoneId);
            slot.alive = false;
            slot.spawning = true;
            slot.mapId = mapId;
            slot.zoneId = zoneId;
            world.spawnWorldBossInZone(mapId, zoneId, request(slot)); // khu đó vừa có boss khác -> failed -> mọc lại sau
         }
      }

      @Override
      public void relocate(int fromMapId) {
         synchronized (FoxService.this) {
            if (slot.thief != thief) {
               return;
            }
            slot.alive = false;
            spawn(slot, fromMapId); // map khác, cùng trạng thái, đầy HP
         }
      }
   }

   // ================================================================== admin panel

   /** Mỗi con: slot, key, name, alive, spawning, mapId, mapName, zoneId, secondsToSpawn. */
   public synchronized List<java.util.Map<String, Object>> adminList() {
      long now = System.currentTimeMillis();
      List<java.util.Map<String, Object>> out = new ArrayList<>();
      for (Slot s : slots) {
         java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
         row.put("slot", s.index);
         row.put("key", KEY_BASE + s.index);
         row.put("name", NAME + " #" + (s.index + 1));
         row.put("alive", s.alive);
         row.put("spawning", s.spawning);
         row.put("mapId", s.mapId);
         row.put("mapName", s.mapName);
         row.put("zoneId", s.zoneId);
         row.put("secondsToSpawn", s.alive || s.spawning ? 0 : Math.max(0, (s.nextSpawnAt - now) / 1000));
         out.add(row);
      }
      return out;
   }

   /** Cho mọc ngay các con chưa có (trong 1 giây). */
   public void adminSpawnAll() {
      synchronized (this) {
         for (Slot slot : slots) {
            slot.nextSpawnAt = 0;
         }
      }
      tick();
   }

   /** Hạ con {@code slot} (vàng đã trộm rơi xuống đất như bị đánh chết, không người kết liễu). */
   public void adminKill(int slot) {
      world.testKillWorldBoss(KEY_BASE + slot);
   }

   // ================================================================== lệnh test

   private static final String USAGE = "Lệnh: /cao [info] | sinh (mọc ngay các con chưa có) | chet N (hạ con N, 0-4)";

   /** Lệnh chat {@code /cao ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      FoxService s = instance;
      if (s == null) {
         session.send(Packets.notice("Chưa bật mini boss Cáo."));
         return;
      }
      String sub = args.length > 1 ? args[1] : "info";
      switch (sub) {
         case "sinh" -> {
            synchronized (s) {
               for (Slot slot : s.slots) {
                  slot.nextSpawnAt = 0;
               }
            }
            s.tick();
            session.send(Packets.notice("Đã cho mọc các con Cáo chưa có."));
         }
         case "chet" -> {
            int n = args.length > 2 ? parse(args[2]) : -1;
            if (n < 0 || n >= COUNT) {
               session.send(Packets.notice(USAGE));
               return;
            }
            s.world.testKillWorldBoss(KEY_BASE + n);
         }
         default -> {
            StringBuilder sb = new StringBuilder("Cáo: ");
            synchronized (s) {
               long now = System.currentTimeMillis();
               for (Slot slot : s.slots) {
                  sb.append('#').append(slot.index).append(' ');
                  if (slot.alive) {
                     sb.append(slot.mapName).append(" (map ").append(slot.mapId).append(") khu ").append(slot.zoneId)
                           .append(", trộm ").append(slot.thief.total());
                  } else {
                     sb.append(slot.spawning ? "đang mọc" : "mọc sau " + Math.max(0, (slot.nextSpawnAt - now) / 1000)
                           + " s");
                  }
                  sb.append("; ");
               }
            }
            session.send(Packets.notice(sb + USAGE));
         }
      }
   }

   private static int parse(String text) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException e) {
         return -1;
      }
   }
}
