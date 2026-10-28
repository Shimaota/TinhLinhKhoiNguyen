package com.tlnt.server.event;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.Rabbit;
import com.tlnt.server.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ngọc thố sự kiện Trung thu: cả server {@link #COUNT} con (chỉ khi sự kiện bật), ở map ngẫu nhiên trong
 * {@link MidAutumn#MAPS} (khu ngẫu nhiên, khu đã có boss người chơi giả thì thử khu khác). Như con cáo trộm
 * ({@code worldboss/FoxService}) nhưng: không đánh ai, không chết, ăn đủ 3 củ cà rốt thì chạy sang map khác
 * ({@code world/Rabbit}). Tắt sự kiện -> gỡ hết ({@code World.despawnWorldBoss}).
 *
 * <p>Luồng: {@link #tick} (scheduler mỗi giây) + listener từ luồng tick khu — trạng thái dưới khoá {@code this}. Chỉ RAM.
 */
public final class RabbitService {

   public static final int COUNT = 5;
   /** Mã boss (admin / lệnh test): 2000 + số thứ tự. */
   static final int KEY_BASE = 2000;
   static final long RETRY_MS = 10_000;
   /** Bật sự kiện / khởi động: các con mọc dần, cách nhau bấy lâu. */
   static final long STAGGER_MS = 2_000;

   /**
    * char_template 21 "Ngọc thố": cấp 1, thanh HP 1.000 (không trừ được), không đánh, không né, tốc chạy = thú cưng
    * 270 px / s, đi dạo đoạn ngắn rồi nghỉ (như thú cưng).
    */
   static final AltarRules.BossSpec SPEC = new AltarRules.BossSpec(0, 0, Rabbit.NAME, 21, 1, 1, 1_000, 1_000, 0,
         new String[0], 0, 0, 0, 0, 0, 0, 0, 0, new int[0], null, List.of(), (long) Rabbit.RUN_SPEED, null);

   private static volatile RabbitService instance;

   private final World world;
   private final Slot[] slots = new Slot[COUNT];

   static final class Slot {
      final int index;
      Rabbit rabbit;
      boolean alive;
      boolean spawning;
      long nextSpawnAt;
      int mapId;
      String mapName = "";
      int zoneId;

      Slot(int index) {
         this.index = index;
         this.rabbit = new Rabbit(index);
      }
   }

   public RabbitService(World world) {
      this.world = world;
      long now = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
         slots[i] = new Slot(i);
         slots[i].nextSpawnAt = now + 60_000 + i * STAGGER_MS;
      }
      instance = this;
      GameEvents.onChange((e, on) -> {
         if (e == GameEvents.Event.MID_AUTUMN) {
            if (on) {
               start();
            } else {
               stop();
            }
         }
      });
   }

   /** Scheduler mỗi giây: sự kiện bật thì mọc các con chưa có. */
   public synchronized void tick() {
      if (!GameEvents.midAutumn()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Slot s : slots) {
         if (!s.alive && !s.spawning && now >= s.nextSpawnAt) {
            spawn(s, -1);
         }
      }
   }

   /** Bật sự kiện: mọc hết trong vài giây. */
   synchronized void start() {
      long now = System.currentTimeMillis();
      for (Slot s : slots) {
         s.nextSpawnAt = now + s.index * STAGGER_MS;
      }
   }

   /** Tắt sự kiện: gỡ mọi con (không quà), lượt mọc cũ thành vô hiệu. */
   synchronized void stop() {
      for (Slot s : slots) {
         world.despawnWorldBoss(KEY_BASE + s.index);
         s.alive = false;
         s.spawning = false;
         s.rabbit = new Rabbit(s.index); // listener của con cũ bị bỏ qua
      }
   }

   /** Gửi yêu cầu mọc con {@code s} ở map ngẫu nhiên khác {@code excludeMapId}. Dưới khoá this. */
   private void spawn(Slot s, int excludeMapId) {
      List<Integer> maps = new ArrayList<>(MidAutumn.MAPS);
      if (maps.size() > 1) {
         maps.remove(Integer.valueOf(excludeMapId));
      }
      int mapId = maps.get(ThreadLocalRandom.current().nextInt(maps.size()));
      s.rabbit = new Rabbit(s.index); // con mới: chưa ăn củ nào
      s.spawning = true;
      Rabbit rabbit = s.rabbit;
      world.spawnWorldBoss(mapId, new World.WorldBossSpawn(KEY_BASE + s.index, Rabbit.NAME, 0, SPEC.level(),
            SPEC.hp(), 0, 0, SPEC, 0, 0, 0, List::of, new Listener(s, rabbit), null, 0, rabbit));
   }

   /** Sự kiện của 1 con (luồng tick khu). Lượt mọc cũ (đã thay / tắt sự kiện) thì bỏ qua. */
   private final class Listener implements World.WorldBossListener {
      private final Slot slot;
      private final Rabbit rabbit;

      Listener(Slot slot, Rabbit rabbit) {
         this.slot = slot;
         this.rabbit = rabbit;
      }

      @Override
      public void spawned(int mapId, String mapName, int zoneId) {
         synchronized (RabbitService.this) {
            if (slot.rabbit != rabbit) {
               world.despawnWorldBoss(KEY_BASE + slot.index); // mọc xong đúng lúc vừa tắt sự kiện
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
         synchronized (RabbitService.this) {
            if (slot.rabbit != rabbit) {
               return;
            }
            slot.spawning = false;
            slot.nextSpawnAt = System.currentTimeMillis() + RETRY_MS;
         }
      }

      @Override
      public void killed(int killerId, String killerName) {
         // không thể bị giết (AltarBoss.damage bỏ qua đòn) — phòng hờ: mọc lại sau
         failed();
      }

      @Override
      public void relocate(int fromMapId) {
         synchronized (RabbitService.this) {
            if (slot.rabbit != rabbit) {
               return;
            }
            Log.debug("Ngoc tho #%d an du ca rot, sang map khac", slot.index);
            slot.alive = false;
            if (GameEvents.midAutumn()) {
               spawn(slot, fromMapId);
            }
         }
      }
   }

   /** Admin panel "Gọi lại Ngọc thố": gỡ cả 5 con rồi cho mọc lại ở map ngẫu nhiên (sự kiện phải đang bật). */
   public static boolean adminRespawnAll() {
      RabbitService s = instance;
      if (s == null || !GameEvents.midAutumn()) {
         return false;
      }
      s.stop();
      s.start();
      return true;
   }

   /** Admin panel: danh sách của dịch vụ đang chạy (rỗng nếu chưa tạo). */
   public static List<Map<String, Object>> adminStatus() {
      RabbitService s = instance;
      return s == null ? List.of() : s.adminList();
   }

   /** Admin panel / lệnh: mỗi con. */
   public synchronized List<Map<String, Object>> adminList() {
      List<Map<String, Object>> out = new ArrayList<>();
      long now = System.currentTimeMillis();
      for (Slot s : slots) {
         Map<String, Object> row = new java.util.LinkedHashMap<>();
         row.put("slot", s.index);
         row.put("name", Rabbit.NAME + " #" + (s.index + 1));
         row.put("status", !GameEvents.midAutumn() ? "Sự kiện tắt" : s.alive ? "Ở " + s.mapName + " khu " + s.zoneId
               : s.spawning ? "Đang xuất hiện" : "Xuất hiện sau " + Math.max(0, (s.nextSpawnAt - now) / 1000) + " s");
         out.add(row);
      }
      return out;
   }
}
