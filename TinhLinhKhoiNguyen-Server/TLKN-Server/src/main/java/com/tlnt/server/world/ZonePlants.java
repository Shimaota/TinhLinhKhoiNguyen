package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.map.PlantSpots;
import com.tlnt.server.net.Packet;
import com.tlnt.server.plant.PlantConfig;
import com.tlnt.server.plant.PlantPackets;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Cây hái được của 1 khu. CHỈ luồng tick của khu đụng tới (như {@link ZoneMobs}).
 *
 * <ul>
 *   <li>Cây đặt tay ({@code map_template.plants}): id thực thể = vị trí trong danh sách, đứng đúng chỗ, bị hái thì
 *       mọc lại đúng chỗ đó sau {@code respawn} giây của điểm.</li>
 *   <li>Cây mọc ngẫu nhiên: {@link SpawnDirector} (toàn server) chọn khu + giành suất rồi giao cho khu mọc
 *       ({@link #spawnRandom}) ở 1 chỗ NGẪU NHIÊN trên mặt đất ({@link PlantSpots}), cách cây khác ≥
 *       {@link #MIN_SPACING_PX}. Id thực thể tăng dần từ {@link #RANDOM_ID_BASE} (không dùng lại id cũ). Bị hái thì
 *       báo {@link SpawnQuota} giữ suất {@code respawn} giây — cây mới sẽ mọc ở map / khu / chỗ ngẫu nhiên.</li>
 *   <li>Hái: người bấm trước GIỮ cây ({@link Plant#harvesterId}) suốt thời gian thu hoạch — người khác bấm vào bị
 *       từ chối. Xong mới mất cây và nhận đồ.</li>
 * </ul>
 */
final class ZonePlants {

   static final int RANDOM_ID_BASE = 10_000;
   /** {@link Plant#fixedIndex} của cây mọc ngẫu nhiên (có suất toàn server). */
   static final int RANDOM = -1;
   /** {@link Plant#fixedIndex} của cây admin đặt thêm: ngoài suất, hái / xoá xong là hết (không mọc lại). */
   static final int ADMIN = -2;
   static final float MIN_SPACING_PX = 150f;
   /** Cập nhật mọc cây mỗi bấy nhiêu ms (không cần mỗi tick). */
   private static final long CHECK_INTERVAL_MS = 1_000;
   private static final int PICK_ATTEMPTS = 30;

   /** 1 cây đang đứng trên map. */
   static final class Plant {
      final int id;
      final int templateId;
      final float x;
      final float y;
      /** Vị trí trong {@code map_template.plants}; {@link #RANDOM} = cây mọc ngẫu nhiên; {@link #ADMIN} = admin đặt. */
      final int fixedIndex;
      /** Người đang hái (0 = không ai) + hạn giữ cây (phòng người hái rời khu mà việc hẹn chưa chạy). */
      int harvesterId;
      long claimUntil;

      Plant(int id, int templateId, float x, float y, int fixedIndex) {
         this.id = id;
         this.templateId = templateId;
         this.x = x;
         this.y = y;
         this.fixedIndex = fixedIndex;
      }

      boolean claimedBy(int playerId, long now) {
         return harvesterId == playerId && now <= claimUntil;
      }

      boolean claimedByOther(int playerId, long now) {
         return harvesterId != 0 && harvesterId != playerId && now <= claimUntil;
      }
   }

   private final int mapId;
   private final List<MapTemplate.PlantSpawn> fixed;
   private final PlantSpots spots;
   /** id thực thể -> cây còn đứng. */
   private final Map<Integer, Plant> alive = new LinkedHashMap<>();
   /** Cây đặt tay đang chờ mọc lại: epoch ms (0 = đang đứng). */
   private final long[] fixedRespawnAt;
   /** Khoá sổ đếm từng khu trong {@link SpawnQuota} (= khu chứa). */
   private final Object quotaKey;
   private int nextRandomId = RANDOM_ID_BASE;
   private long nextCheckAt;
   /** Gói op 54 sub 0 cho người vào khu; null = có thay đổi, dựng lại khi cần. */
   private Packet snapshot;

   ZonePlants(MapTemplate map, PlantSpots spots) {
      this(map, spots, null);
   }

   ZonePlants(MapTemplate map, PlantSpots spots, Object quotaKey) {
      this.quotaKey = quotaKey != null ? quotaKey : this;
      this.mapId = map.id();
      this.fixed = map.plants();
      this.spots = spots;
      this.fixedRespawnAt = new long[fixed.size()];
      for (int i = 0; i < fixed.size(); i++) {
         MapTemplate.PlantSpawn s = fixed.get(i);
         alive.put(i, new Plant(i, s.id(), s.x(), s.y(), i));
      }
   }

   Plant get(int plantId) {
      return alive.get(plantId);
   }

   Iterable<Plant> alive() {
      return alive.values();
   }

   int spotCount() {
      return spots.size();
   }

   /** Cây đặt tay tới giờ thì mọc lại đúng chỗ; {@code force} (lệnh test) = mọc ngay. Phát op 54 sub 1. */
   void update(long now, boolean force, Consumer<Packet> broadcast) {
      if (!force && now < nextCheckAt) {
         return;
      }
      nextCheckAt = now + CHECK_INTERVAL_MS;
      for (int i = 0; i < fixedRespawnAt.length; i++) {
         if (fixedRespawnAt[i] > 0 && (force || now >= fixedRespawnAt[i])) {
            fixedRespawnAt[i] = 0;
            MapTemplate.PlantSpawn s = fixed.get(i);
            add(new Plant(i, s.id(), s.x(), s.y(), i), broadcast);
         }
      }
   }

   /**
    * Mọc 1 cây ngẫu nhiên loại {@code entry} (suất đã được {@link SpawnDirector} giành). @return false nếu không còn
    * chỗ trống (người gọi trả suất)
    */
   boolean spawnRandom(PlantConfig.Entry entry, Consumer<Packet> broadcast) {
      float[] spot = pickSpot();
      if (spot == null) {
         return false;
      }
      add(new Plant(nextRandomId++, entry.templateId(), spot[0], spot[1], RANDOM), broadcast);
      return true;
   }

   /**
    * Cây vừa bị hái xong: gỡ khỏi map, phát op 54 sub 3 (người hái làm động tác PICK, cây biến mất), hẹn mọc lại.
    */
   void harvested(Plant plant, int playerId, long now, Consumer<Packet> broadcast) {
      if (alive.remove(plant.id) == null) {
         return;
      }
      snapshot = null;
      broadcast.accept(PlantPackets.picked(playerId, plant.id).toPacket());
      afterRemoved(plant, now);
   }

   /** Cây vừa mất (hái / admin xoá): đặt tay -> hẹn mọc lại đúng chỗ; ngẫu nhiên -> giữ suất; admin đặt -> hết. */
   private void afterRemoved(Plant plant, long now) {
      if (plant.fixedIndex >= 0) {
         fixedRespawnAt[plant.fixedIndex] = now + Math.max(1, fixed.get(plant.fixedIndex).respawn()) * 1000L;
         return;
      }
      if (plant.fixedIndex == ADMIN) {
         return;
      }
      PlantConfig.Entry entry = PlantConfig.current().entry(plant.templateId);
      long respawnAt = now + (entry != null ? entry.respawnDelayMs() : 60_000);
      SpawnQuota.plant(plant.templateId).killed(quotaKey, respawnAt); // giữ suất toàn server tới lúc mọc lại
   }

   /**
    * Admin xoá cây {@code plantId}: biến mất như bị hái (op 54 sub 3, không người hái) rồi tính như đã hái (đặt tay mọc
    * lại đúng chỗ sau respawn, ngẫu nhiên giữ suất rồi mọc chỗ khác, admin đặt thì hết). @return false = không còn cây
    */
   boolean adminRemove(int plantId, long now, Consumer<Packet> broadcast) {
      Plant plant = alive.remove(plantId);
      if (plant == null) {
         return false;
      }
      snapshot = null;
      broadcast.accept(PlantPackets.picked(0, plant.id).toPacket());
      afterRemoved(plant, now);
      return true;
   }

   /**
    * Admin đặt thêm 1 cây loại {@code templateId} ở ({@code x}, {@code y}) — null = chỗ ngẫu nhiên trên mặt đất (cách cây
    * khác ≥ {@link #MIN_SPACING_PX}). Ngoài suất toàn server, không mọc lại. @return id cây, -1 = không còn chỗ
    */
   int adminSpawn(int templateId, Float x, Float y, Consumer<Packet> broadcast) {
      float[] spot = x != null && y != null ? new float[] {x, y} : pickSpot();
      if (spot == null) {
         return -1;
      }
      Plant plant = new Plant(nextRandomId++, templateId, spot[0], spot[1], ADMIN);
      add(plant, broadcast);
      return plant.id;
   }

   /** Toàn bộ cây đang đứng (op 54 sub 0), mã hoá lại chỉ khi có thay đổi. */
   Packet snapshot() {
      Packet packet = snapshot;
      if (packet == null) {
         int n = alive.size();
         int[] ids = new int[n];
         float[] xs = new float[n];
         float[] ys = new float[n];
         int[] templates = new int[n];
         int i = 0;
         for (Plant p : alive.values()) {
            ids[i] = p.id;
            xs[i] = p.x;
            ys[i] = p.y;
            templates[i++] = p.templateId;
         }
         packet = PlantPackets.list(n, ids, xs, ys, templates).toPacket();
         snapshot = packet;
      }
      return packet;
   }

   /** Khu đóng (luồng tick của khu): trả suất toàn server của mọi cây ngẫu nhiên đang đứng. */
   void releaseAll() {
      for (Plant p : alive.values()) {
         if (p.fixedIndex == RANDOM) {
            SpawnQuota.plant(p.templateId).release(quotaKey);
         }
      }
      alive.clear();
      snapshot = null;
   }

   /** Số cây ngẫu nhiên loại này đang đứng trên toàn server. */
   static int serverCount(int templateId) {
      return SpawnQuota.plant(templateId).alive();
   }

   int countRandom(int templateId) {
      int n = 0;
      for (Plant p : alive.values()) {
         if (p.fixedIndex == RANDOM && p.templateId == templateId) {
            n++;
         }
      }
      return n;
   }

   private void add(Plant plant, Consumer<Packet> broadcast) {
      alive.put(plant.id, plant);
      snapshot = null;
      broadcast.accept(PlantPackets.spawn(plant.id, plant.x, plant.y, plant.templateId).toPacket());
   }

   /** Chỗ ngẫu nhiên cách mọi cây đang đứng ≥ {@link #MIN_SPACING_PX}; null nếu không tìm được. */
   private float[] pickSpot() {
      int n = spots.size();
      if (n == 0) {
         return null;
      }
      ThreadLocalRandom random = ThreadLocalRandom.current();
      for (int attempt = 0; attempt < PICK_ATTEMPTS; attempt++) {
         int i = random.nextInt(n);
         float x = spots.x(i);
         float y = spots.y(i);
         if (farFromPlants(x, y)) {
            return new float[] {x, y};
         }
      }
      return null;
   }

   private boolean farFromPlants(float x, float y) {
      for (Iterator<Plant> it = alive.values().iterator(); it.hasNext(); ) {
         Plant p = it.next();
         if (Math.abs(p.x - x) < MIN_SPACING_PX && Math.abs(p.y - y) < MIN_SPACING_PX) {
            return false;
         }
      }
      return true;
   }
}
