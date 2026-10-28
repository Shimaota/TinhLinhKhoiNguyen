package com.tlnt.server.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Chỗ cây mọc ngẫu nhiên được của 1 map: các điểm trên MẶT TRÊN của collider hộp (đất / nền 1 chiều — người đứng
 * được), cách nhau {@link #STEP_PX}. Bỏ điểm:
 * <ul>
 *   <li>sát mép hộp ({@link #EDGE_PX}), sát mép map ({@link #MAP_EDGE_PX}), trên trần map;</li>
 *   <li>bị collider đặc khác che trong {@link #HEADROOM_PX} phía trên (điểm nằm trong khối đất / dưới vách);</li>
 *   <li>gần cửa dịch chuyển ({@link #WAYPOINT_GAP_PX}), gần NPC ({@link #NPC_GAP_PX}), gần điểm xuất hiện của map,
 *       gần mặt nước.</li>
 * </ul>
 * Dốc (tam giác) không mọc cây. Tính 1 lần khi mở map (bất biến, dùng chung mọi khu). Toạ độ = chân cây (như cột
 * {@code plants}: y = mặt đất).
 */
public final class PlantSpots {

   static final float STEP_PX = 60f;
   static final float EDGE_PX = 40f;
   /** Cách mép trái / phải map (vách biên, người chơi khó tới sát). */
   static final float MAP_EDGE_PX = 150f;
   static final float HEADROOM_PX = 120f;
   static final float WAYPOINT_GAP_PX = 350f;
   static final float NPC_GAP_PX = 180f;
   static final float SPAWN_GAP_PX = 200f;
   static final float WATER_GAP_PX = 60f;

   private final float[] xs;
   private final float[] ys;

   private PlantSpots(float[] xs, float[] ys) {
      this.xs = xs;
      this.ys = ys;
   }

   public int size() {
      return xs.length;
   }

   public float x(int i) {
      return xs[i];
   }

   public float y(int i) {
      return ys[i];
   }

   /**
    * Mặt đất ngay dưới điểm ({@code x}, {@code y}): mặt trên cao nhất của collider hộp chứa {@code x} và không cao hơn
    * {@code y}; không có thì giữ {@code y}. Dùng đặt vật phẩm rơi (client tự rơi theo trọng lực tới đúng chỗ này).
    */
   public static float groundBelow(MapTemplate map, float x, float y) {
      float best = Float.NEGATIVE_INFINITY;
      for (MapTemplate.Collider c : map.colliders()) {
         if (c.triangle() || x < c.x() || x > c.x() + c.w()) {
            continue;
         }
         float top = c.y() + c.h();
         if (top <= y + 1 && top > best) {
            best = top;
         }
      }
      return best == Float.NEGATIVE_INFINITY ? y : best;
   }

   public static PlantSpots of(MapTemplate map) {
      List<float[]> points = new ArrayList<>();
      List<MapTemplate.Collider> colliders = map.colliders();
      for (MapTemplate.Collider c : colliders) {
         if (c.triangle() || c.w() < EDGE_PX * 2) {
            continue;
         }
         float top = c.y() + c.h();
         if (top < 0 || top > map.height() - HEADROOM_PX) {
            continue; // trần map / ngoài map
         }
         float from = Math.max(c.x() + EDGE_PX, MAP_EDGE_PX);
         float to = Math.min(c.x() + c.w() - EDGE_PX, map.width() - MAP_EDGE_PX);
         for (float x = from; x <= to; x += STEP_PX) {
            if (usable(map, c, x, top)) {
               points.add(new float[] {x, top});
            }
         }
      }
      float[] xs = new float[points.size()];
      float[] ys = new float[points.size()];
      for (int i = 0; i < xs.length; i++) {
         xs[i] = points.get(i)[0];
         ys[i] = points.get(i)[1];
      }
      return new PlantSpots(xs, ys);
   }

   private static boolean usable(MapTemplate map, MapTemplate.Collider ground, float x, float y) {
      for (MapTemplate.Collider c : map.colliders()) {
         if (c == ground || c.oneWay()) {
            continue;
         }
         float left;
         float right;
         float bottom;
         float topEdge;
         if (c.triangle()) {
            float[] p = c.points();
            left = Math.min(p[0], Math.min(p[2], p[4]));
            right = Math.max(p[0], Math.max(p[2], p[4]));
            bottom = Math.min(p[1], Math.min(p[3], p[5]));
            topEdge = Math.max(p[1], Math.max(p[3], p[5]));
         } else {
            left = c.x();
            right = c.x() + c.w();
            bottom = c.y();
            topEdge = c.y() + c.h();
         }
         // Khối đặc chiếm khoảng (y, y + HEADROOM] ngay trên điểm -> điểm nằm trong đất / dưới vách.
         if (x > left && x < right && topEdge > y + 1 && bottom < y + HEADROOM_PX) {
            return false;
         }
      }
      for (MapTemplate.Waypoint wp : map.waypoints()) {
         if (wp.contains(x, y, WAYPOINT_GAP_PX)) {
            return false;
         }
      }
      for (MapTemplate.NpcSpawn npc : map.npcs()) {
         if (Math.abs(npc.x() - x) < NPC_GAP_PX && Math.abs(npc.y() - y) < NPC_GAP_PX) {
            return false;
         }
      }
      if (Math.abs(map.spawnX() - x) < SPAWN_GAP_PX && Math.abs(map.spawnY() - y) < SPAWN_GAP_PX) {
         return false;
      }
      return !map.nearWater(x, y, WATER_GAP_PX);
   }
}
