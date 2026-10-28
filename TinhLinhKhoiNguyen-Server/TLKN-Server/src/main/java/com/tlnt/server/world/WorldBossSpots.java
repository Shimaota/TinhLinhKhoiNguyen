package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Chọn chỗ xuất hiện ngẫu nhiên cho boss thế giới trên 1 map: chỗ ĐỨNG ĐƯỢC (mặt trên của nền / dốc — lấy quanh điểm sinh
 * quái của map, nơi chắc chắn có đất và người hay qua lại) và XA cửa chuyển map ({@link #MIN_WAYPOINT_PX}) + điểm vào
 * map ({@link #MIN_SPAWN_PX}) — người vừa qua cửa không bị boss đánh ngay. Bất biến, không trạng thái.
 */
final class WorldBossSpots {

   /** Cách mép hộp cửa chuyển map ít nhất bấy nhiêu px (khoảng cách ngang + dọc). */
   static final float MIN_WAYPOINT_PX = 700f;
   /** Cách điểm vào map ít nhất bấy nhiêu px. */
   static final float MIN_SPAWN_PX = 500f;
   /** Lệch ngang ngẫu nhiên quanh điểm sinh quái. */
   static final float JITTER_PX = 200f;
   /** Thử bấy nhiêu lần trước khi nới điều kiện. */
   static final int TRIES = 24;

   private WorldBossSpots() {
   }

   /** Chỗ đứng {x, y} cho boss; map không có điểm sinh quái -> mặt trên 1 nền ngẫu nhiên; không có gì -> điểm vào map. */
   static float[] pick(MapTemplate map, Random random) {
      List<float[]> anchors = new ArrayList<>();
      for (MapTemplate.MobSpawn mob : map.mobs()) {
         anchors.add(new float[] {mob.x(), mob.y()});
      }
      if (anchors.isEmpty()) {
         for (MapTemplate.Collider c : map.colliders()) {
            if (!c.triangle() && c.w() >= 200) {
               anchors.add(new float[] {c.x() + c.w() / 2, c.y() + c.h()});
            }
         }
      }
      if (anchors.isEmpty()) {
         return new float[] {map.spawnX(), map.spawnY()};
      }
      float[] fallback = null;
      for (int i = 0; i < TRIES; i++) {
         float[] a = anchors.get(random.nextInt(anchors.size()));
         float x = map.clampX(a[0] + (random.nextFloat() * 2 - 1) * JITTER_PX);
         float ground = Pet.surfaceBelow(map, x, a[1] + BossMover.STEP_PX);
         if (Float.isNaN(ground) || Math.abs(ground - a[1]) > 150) { // lệch ra ngoài mép / xuống hố: đứng đúng điểm sinh
            x = a[0];
            ground = Pet.surfaceBelow(map, x, a[1] + BossMover.STEP_PX);
            if (Float.isNaN(ground)) {
               ground = a[1];
            }
         }
         float[] spot = {x, ground};
         if (farFromDoors(map, x, ground)) {
            return spot;
         }
         if (fallback == null) {
            fallback = spot;
         }
      }
      return fallback; // map nhỏ, chỗ nào cũng gần cửa: vẫn phải ra
   }

   /** Xa mọi cửa chuyển map + điểm vào map. */
   static boolean farFromDoors(MapTemplate map, float x, float y) {
      if (Math.abs(x - map.spawnX()) + Math.abs(y - map.spawnY()) < MIN_SPAWN_PX) {
         return false;
      }
      for (MapTemplate.Waypoint w : map.waypoints()) {
         float dx = Math.max(0, Math.max(w.x() - x, x - (w.x() + w.w())));
         float dy = Math.max(0, Math.max(w.y() - y, y - (w.y() + w.h())));
         if (dx + dy < MIN_WAYPOINT_PX) {
            return false;
         }
      }
      return true;
   }
}
