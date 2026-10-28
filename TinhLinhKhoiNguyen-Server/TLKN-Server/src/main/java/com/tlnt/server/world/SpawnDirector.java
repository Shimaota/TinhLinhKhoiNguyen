package com.tlnt.server.world;

import com.tlnt.server.gather.GatherConfig;
import com.tlnt.server.plant.PlantConfig;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Rải cây mọc ngẫu nhiên + tổ ong / quả dừa ra TOÀN SERVER, không phụ thuộc khu nào đang có người.
 *
 * <p>Mỗi {@link #INTERVAL_SECONDS} giây (luồng scheduler), với từng loại: còn suất ({@code total} trừ đang đứng trừ đang
 * hồi — {@link SpawnQuota}) thì chọn map đang có ÍT NHẤT loại này (bằng nhau thì ngẫu nhiên) -> chia đều các map
 * (10 con / 4 map = 3-3-2-2), rồi 1 khu cố định ngẫu nhiên của map đó còn chưa đủ giới hạn / khu, giành suất và giao cho khu mọc ở 1 chỗ ngẫu nhiên
 * ({@code zone.submit} — khu trống cũng chạy lệnh). Ví dụ total 10: đúng 10 cái nằm rải rác ở các map / khu, người
 * chơi phải đi tìm; bị hái / phá thì sau {@code respawn} giây 1 cái mới mọc ở map / khu / chỗ ngẫu nhiên.
 *
 * <p>Chỉ khu cố định (0 .. zone_count-1) của map thường; khu mở thêm lúc đông người và map riêng tư không có.
 * {@code total} = 0 (không giới hạn): mọi khu đều đủ giới hạn / khu.
 */
final class SpawnDirector {

   static final int INTERVAL_SECONDS = 1;

   private SpawnDirector() {
   }

   /** 1 lượt rải (luồng scheduler). */
   static void run(World world, long now) {
      try {
         for (PlantConfig.Entry e : PlantConfig.current().byTemplate().values()) {
            List<List<Zone>> byMap = new ArrayList<>();
            for (int mapId : e.maps()) {
               GameMap map = world.map(mapId);
               if (map != null && !map.isPrivate() && map.plantSpots().size() > 0) {
                  byMap.add(map.permanentZones());
               }
            }
            SpawnQuota quota = SpawnQuota.plant(e.templateId());
            fill(quota, e.total(), e.max(), byMap, now, zone -> zone.submit(() -> {
               if (!zone.plants().spawnRandom(e, zone.broadcastAll())) {
                  quota.release(zone); // hết chỗ trống: trả suất, lượt sau thử khu khác
               }
            }));
         }
         for (GatherConfig.Entry e : GatherConfig.current().byMob().values()) {
            List<List<Zone>> byMap = new ArrayList<>();
            for (int mapId : e.spots().keySet()) {
               GameMap map = world.map(mapId);
               if (map != null && !map.isPrivate() && !e.spots(mapId).isEmpty()) {
                  byMap.add(map.permanentZones());
               }
            }
            SpawnQuota quota = SpawnQuota.mob(e.mobId());
            fill(quota, e.total(), e.perZone(), byMap, now, zone -> zone.submit(() -> {
               if (!zone.mobs().spawnGather(e, zone.broadcastAll())) {
                  quota.release(zone);
               }
            }));
         }
      } catch (RuntimeException ex) {
         Log.error("Loi rai cay / to ong", ex);
      }
   }

   /**
    * Giao suất còn trống: mỗi lần chọn map đang có ít nhất (hoà -> ngẫu nhiên), rồi 1 khu ngẫu nhiên của map còn chưa
    * đủ {@code zoneMax}; map đã kín mọi khu thì bỏ khỏi lượt. @return số cái đã giao
    */
   static <Z> int fill(SpawnQuota quota, int total, int zoneMax, List<List<Z>> byMap, long now, Consumer<Z> spawn) {
      List<List<Z>> maps = new ArrayList<>();
      for (List<Z> zones : byMap) {
         if (!zones.isEmpty()) {
            maps.add(zones);
         }
      }
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int placed = 0;
      while (!maps.isEmpty() && quota.hasRoom(total, now)) {
         int m = leastFilled(quota, maps, random);
         List<Z> open = new ArrayList<>();
         for (Z zone : maps.get(m)) {
            if (quota.inZone(zone) < zoneMax) {
               open.add(zone);
            }
         }
         if (open.isEmpty()) {
            maps.remove(m);
            continue;
         }
         Z zone = open.get(random.nextInt(open.size()));
         if (quota.reserve(zone, total, zoneMax, now)) {
            spawn.accept(zone);
            placed++;
         }
      }
      return placed;
   }

   /** Vị trí map đang có ít cái nhất (cộng mọi khu); nhiều map bằng nhau thì chọn ngẫu nhiên 1 map. */
   private static <Z> int leastFilled(SpawnQuota quota, List<List<Z>> maps, ThreadLocalRandom random) {
      int best = Integer.MAX_VALUE;
      int pick = 0;
      int ties = 0;
      for (int i = 0; i < maps.size(); i++) {
         int n = 0;
         for (Z zone : maps.get(i)) {
            n += quota.inZone(zone);
         }
         if (n < best) {
            best = n;
            pick = i;
            ties = 1;
         } else if (n == best && random.nextInt(++ties) == 0) { // chọn đều giữa các map hoà
            pick = i;
         }
      }
      return pick;
   }
}
