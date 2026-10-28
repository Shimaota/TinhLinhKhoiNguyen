package com.tlnt.server.world;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sổ đếm TOÀN SERVER của 1 loại cây mọc ngẫu nhiên / quái săn bắt (tổ ong, quả dừa):
 * <ul>
 *   <li>{@code alive}: đang đứng (cả server) + đếm theo từng khu ({@code key} = khu) để giữ giới hạn / khu;</li>
 *   <li>{@code cooling}: suất của cái vừa bị hái / phá, giữ tới hết {@code respawn} giây rồi mới trả.</li>
 * </ul>
 * {@link SpawnDirector} (luồng scheduler) giành suất rồi mới giao cho khu mọc; khu (luồng tick) báo lại khi bị hái /
 * phá / khu đóng. Nhiều luồng dùng -> khoá theo từng loại (việc rất nhẹ, hiếm).
 */
final class SpawnQuota {

   private static final Map<Integer, SpawnQuota> BY_MOB = new ConcurrentHashMap<>();
   private static final Map<Integer, SpawnQuota> BY_PLANT = new ConcurrentHashMap<>();

   private int alive;
   private final Map<Object, Integer> perZone = new HashMap<>();
   /** Giờ trả suất (epoch ms) của các cái đã bị hái / phá, theo thứ tự. */
   private final ArrayDeque<Long> cooling = new ArrayDeque<>();

   /** Quái săn bắt theo mob_template.id. */
   static SpawnQuota mob(int mobId) {
      return BY_MOB.computeIfAbsent(mobId, k -> new SpawnQuota());
   }

   /** Cây mọc ngẫu nhiên theo plant_template.id. */
   static SpawnQuota plant(int templateId) {
      return BY_PLANT.computeIfAbsent(templateId, k -> new SpawnQuota());
   }

   /** Test: xoá sạch đếm của mọi loại. */
   static void resetAll() {
      BY_MOB.clear();
      BY_PLANT.clear();
   }

   /** Còn được mọc thêm không (chưa đủ {@code total}; ≤ 0 = không giới hạn tổng). */
   synchronized boolean hasRoom(int total, long now) {
      drain(now);
      return total <= 0 || alive + cooling.size() < total;
   }

   /** Giành 1 suất ở khu {@code key}: còn chỗ toàn server + khu chưa đủ {@code zoneMax}. */
   synchronized boolean reserve(Object key, int total, int zoneMax, long now) {
      if (!hasRoom(total, now) || perZone.getOrDefault(key, 0) >= zoneMax) {
         return false;
      }
      alive++;
      perZone.merge(key, 1, Integer::sum);
      return true;
   }

   /** Bị hái / phá ở khu {@code key}: giữ suất toàn server tới {@code releaseAt}. */
   synchronized void killed(Object key, long releaseAt) {
      if (take(key)) {
         cooling.addLast(releaseAt);
      }
   }

   /** Trả suất ngay (khu không đặt được chỗ / khu đóng). */
   synchronized void release(Object key) {
      take(key);
   }

   synchronized int inZone(Object key) {
      return perZone.getOrDefault(key, 0);
   }

   /** Lệnh test: bỏ hết thời gian hồi. */
   synchronized void clearCooling() {
      cooling.clear();
   }

   synchronized int alive() {
      return alive;
   }

   synchronized int cooling(long now) {
      drain(now);
      return cooling.size();
   }

   private boolean take(Object key) {
      Integer n = perZone.get(key);
      if (n == null) {
         return false;
      }
      if (n <= 1) {
         perZone.remove(key);
      } else {
         perZone.put(key, n - 1);
      }
      alive = Math.max(0, alive - 1);
      return true;
   }

   private void drain(long now) {
      while (!cooling.isEmpty() && cooling.peekFirst() <= now) {
         cooling.pollFirst();
      }
   }
}
