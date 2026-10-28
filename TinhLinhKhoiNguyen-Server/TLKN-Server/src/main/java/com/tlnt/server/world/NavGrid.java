package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Bản đồ đi bộ của 1 map cho boss do server điều khiển ({@link BossMover}) — boss CHẠY trên mặt đất, nhảy lên / rơi
 * xuống theo địa hình 2D như người chơi / thú cưng, không bay xuyên địa hình.
 *
 * <p>Map chia cột {@link #COL_PX} px; mỗi cột có các MẶT ĐỨNG ĐƯỢC (mặt trên hộp va chạm / nền 1 chiều / cạnh trên dốc,
 * trên đầu còn chỗ cho thân {@link #BODY_PX}). Mỗi mặt = 1 nút. Cạnh nối:
 * <ul>
 *   <li>{@link #WALK}: sang cột bên cạnh, chênh ≤ {@link #WALK_RISE_PX} (dốc / bậc thấp), không vướng tường.</li>
 *   <li>{@link #FALL}: chạy ra mép, rơi xuống mặt cao nhất bên dưới ở cột bên cạnh.</li>
 *   <li>{@link #DROP}: đang đứng trên nền 1 chiều -> rơi xuyên xuống mặt ngay dưới.</li>
 *   <li>{@link #JUMP}: nhảy lên mặt cao hơn (≤ {@link #MAX_JUMP_PX}) cách tối đa {@link #JUMP_COLS} cột, trên đầu chỗ cất
 *       cánh không có trần đặc chắn.</li>
 * </ul>
 * Tìm đường = Dijkstra ({@link #path}). Bất biến sau khi dựng, dùng chung mọi luồng (cache theo map).
 */
final class NavGrid {

   static final float COL_PX = 40f;
   static final float BODY_PX = 80f;
   static final float WALK_RISE_PX = 45f;
   /** Nhảy cao tối đa (như {@code PetBrain}: thú cưng nhảy lên bậc ≤ 650 px). */
   static final float MAX_JUMP_PX = 600f;
   /** Nhảy xa tối đa bấy nhiêu cột (≈ 280 px). */
   static final int JUMP_COLS = 7;

   static final byte WALK = 0;
   static final byte FALL = 1;
   static final byte DROP = 2;
   static final byte JUMP = 3;

   /** Theo ĐÚNG đối tượng mẫu map (record so sánh sâu — băm cả danh sách va chạm thì tốn). */
   private static final Map<MapTemplate, NavGrid> CACHE = new java.util.IdentityHashMap<>();

   final float[] nodeX;
   final float[] nodeY;
   final boolean[] oneWay;
   private final int[] nodeCol;
   /** Nút của từng cột (theo y tăng dần). */
   private final int[][] colNodes;
   private final int[][] edgeTo;
   private final byte[][] edgeKind;
   private final float[][] edgeCost;

   static NavGrid of(MapTemplate map) {
      synchronized (CACHE) {
         return CACHE.computeIfAbsent(map, NavGrid::new);
      }
   }

   private NavGrid(MapTemplate map) {
      int cols = Math.max(1, (int) Math.ceil(map.width() / COL_PX) + 1);
      List<float[]> nodes = new ArrayList<>(); // {x, y, col, oneWay}
      colNodes = new int[cols][];
      for (int c = 0; c < cols; c++) {
         float x = map.clampX(c * COL_PX);
         List<Float> tops = surfaces(map, x);
         int[] ids = new int[tops.size()];
         for (int k = 0; k < tops.size(); k++) {
            float y = tops.get(k);
            ids[k] = nodes.size();
            nodes.add(new float[] {x, y, c, onlyOneWay(map, x, y) ? 1 : 0});
         }
         colNodes[c] = ids;
      }
      int n = nodes.size();
      nodeX = new float[n];
      nodeY = new float[n];
      oneWay = new boolean[n];
      nodeCol = new int[n];
      for (int i = 0; i < n; i++) {
         float[] f = nodes.get(i);
         nodeX[i] = f[0];
         nodeY[i] = f[1];
         nodeCol[i] = (int) f[2];
         oneWay[i] = f[3] != 0;
      }
      edgeTo = new int[n][];
      edgeKind = new byte[n][];
      edgeCost = new float[n][];
      for (int a = 0; a < n; a++) {
         buildEdges(map, a, cols);
      }
   }

   /** Mặt đứng được tại {@code x} (dưới lên, gộp các mặt sát nhau). */
   private static List<Float> surfaces(MapTemplate map, float x) {
      List<Float> raw = new ArrayList<>();
      for (MapTemplate.Collider c : map.colliders()) {
         float top;
         if (c.triangle()) {
            top = Pet.triangleTop(c.points(), x);
         } else if (x >= c.x() && x <= c.x() + c.w()) {
            top = c.y() + c.h();
         } else {
            continue;
         }
         if (!Float.isNaN(top) && top <= map.height() && clear(map, x, top)) {
            raw.add(top);
         }
      }
      raw.sort(Float::compare);
      List<Float> out = new ArrayList<>();
      for (float y : raw) {
         if (out.isEmpty() || y - out.get(out.size() - 1) > 8f) {
            out.add(y);
         } else {
            out.set(out.size() - 1, y); // mặt sát nhau: lấy mặt trên
         }
      }
      return out;
   }

   /** Trên mặt {@code y} tại {@code x} còn chỗ cho thân (không bị hộp đặc khác đè lên). */
   private static boolean clear(MapTemplate map, float x, float y) {
      for (MapTemplate.Collider c : map.colliders()) {
         if (c.triangle() || c.oneWay() || x <= c.x() || x >= c.x() + c.w()) {
            continue;
         }
         if (c.y() < y + BODY_PX && c.y() + c.h() > y + 2f) {
            return false;
         }
      }
      return true;
   }

   private static boolean onlyOneWay(MapTemplate map, float x, float y) {
      return Escort.standingOnOneWay(map, x, y);
   }

   private void buildEdges(MapTemplate map, int a, int cols) {
      List<int[]> to = new ArrayList<>(); // {node, kind}
      List<Float> cost = new ArrayList<>();
      int c = nodeCol[a];
      float ya = nodeY[a];
      for (int side = -1; side <= 1; side += 2) {
         int cb = c + side;
         if (cb < 0 || cb >= cols) {
            continue;
         }
         int walk = -1;
         for (int b : colNodes[cb]) {
            if (Math.abs(nodeY[b] - ya) <= WALK_RISE_PX) {
               walk = b;
            }
         }
         if (walk >= 0) {
            to.add(new int[] {walk, WALK});
            cost.add(1f);
         } else {
            int below = highestBelow(cb, ya - WALK_RISE_PX);
            if (below >= 0) {
               to.add(new int[] {below, FALL});
               cost.add(1f + (ya - nodeY[below]) / 300f);
            }
         }
      }
      if (oneWay[a]) {
         int below = highestBelow(c, ya - 8f);
         if (below >= 0) {
            to.add(new int[] {below, DROP});
            cost.add(1.5f + (ya - nodeY[below]) / 300f);
         }
      }
      if (!ceilingAbove(map, nodeX[a], ya)) {
         for (int cb = Math.max(0, c - JUMP_COLS); cb <= Math.min(cols - 1, c + JUMP_COLS); cb++) {
            for (int b : colNodes[cb]) {
               float rise = nodeY[b] - ya;
               if (rise > WALK_RISE_PX && rise <= MAX_JUMP_PX && (cb != c || oneWay[b])) {
                  to.add(new int[] {b, JUMP});
                  cost.add(2f + Math.abs(cb - c) * 0.5f + rise / 300f);
               }
            }
         }
      }
      edgeTo[a] = new int[to.size()];
      edgeKind[a] = new byte[to.size()];
      edgeCost[a] = new float[to.size()];
      for (int i = 0; i < to.size(); i++) {
         edgeTo[a][i] = to.get(i)[0];
         edgeKind[a][i] = (byte) to.get(i)[1];
         edgeCost[a][i] = cost.get(i);
      }
   }

   /** Trên đầu chỗ đứng ({@code x}, {@code y}) có hộp đặc thấp (trần) -> không nhảy được. */
   private static boolean ceilingAbove(MapTemplate map, float x, float y) {
      for (MapTemplate.Collider c : map.colliders()) {
         if (c.triangle() || c.oneWay() || x <= c.x() || x >= c.x() + c.w()) {
            continue;
         }
         if (c.y() >= y + BODY_PX && c.y() < y + BODY_PX + 120f) {
            return true;
         }
      }
      return false;
   }

   /** Mặt cao nhất của cột {@code col} thấp hơn {@code maxY}; -1 = không có. */
   private int highestBelow(int col, float maxY) {
      int best = -1;
      for (int b : colNodes[col]) {
         if (nodeY[b] < maxY && (best < 0 || nodeY[b] > nodeY[best])) {
            best = b;
         }
      }
      return best;
   }

   /** Nút đang đứng / gần nhất dưới chân tại ({@code x}, {@code y}); -1 = cột không có mặt nào. */
   int nodeAt(MapTemplate map, float x, float y) {
      int col = Math.max(0, Math.min(colNodes.length - 1, Math.round(map.clampX(x) / COL_PX)));
      int best = -1;
      for (int dc = 0; dc <= 2 && best < 0; dc++) {
         for (int s = -1; s <= 1; s += 2) {
            int cc = col + dc * s;
            if (cc < 0 || cc >= colNodes.length) {
               continue;
            }
            for (int b : colNodes[cc]) {
               if (nodeY[b] <= y + WALK_RISE_PX && (best < 0 || nodeY[b] > nodeY[best])) {
                  best = b;
               }
            }
            if (dc == 0) {
               break;
            }
         }
      }
      return best;
   }

   /** Loại cạnh {@code a} -> {@code b}; -1 = không nối. */
   byte kind(int a, int b) {
      for (int i = 0; i < edgeTo[a].length; i++) {
         if (edgeTo[a][i] == b) {
            return edgeKind[a][i];
         }
      }
      return -1;
   }

   /** Đường đi ngắn nhất (danh sách nút, gồm cả 2 đầu); null = không tới được. */
   int[] path(int from, int to) {
      if (from < 0 || to < 0) {
         return null;
      }
      if (from == to) {
         return new int[] {from};
      }
      int n = nodeX.length;
      float[] dist = new float[n];
      int[] prev = new int[n];
      Arrays.fill(dist, Float.MAX_VALUE);
      Arrays.fill(prev, -1);
      dist[from] = 0;
      PriorityQueue<float[]> queue = new PriorityQueue<>((p, q) -> Float.compare(p[0], q[0]));
      queue.add(new float[] {0, from});
      while (!queue.isEmpty()) {
         float[] top = queue.poll();
         int a = (int) top[1];
         if (top[0] > dist[a]) {
            continue;
         }
         if (a == to) {
            break;
         }
         for (int i = 0; i < edgeTo[a].length; i++) {
            int b = edgeTo[a][i];
            float d = dist[a] + edgeCost[a][i];
            if (d < dist[b]) {
               dist[b] = d;
               prev[b] = a;
               queue.add(new float[] {d, b});
            }
         }
      }
      if (prev[to] < 0) {
         return null;
      }
      int len = 1;
      for (int v = to; v != from; v = prev[v]) {
         len++;
      }
      int[] out = new int[len];
      int v = to;
      for (int i = len - 1; i >= 0; i--) {
         out[i] = v;
         v = prev[v];
      }
      return out;
   }

   /** Mọi nút tới được từ {@code from} (để chọn chỗ chạy trốn / đi tuần khắp map). */
   int[] reachable(int from) {
      if (from < 0) {
         return new int[0];
      }
      boolean[] seen = new boolean[nodeX.length];
      int[] stack = new int[nodeX.length];
      int top = 0;
      int count = 0;
      stack[top++] = from;
      seen[from] = true;
      int[] out = new int[nodeX.length];
      while (top > 0) {
         int a = stack[--top];
         out[count++] = a;
         for (int b : edgeTo[a]) {
            if (!seen[b]) {
               seen[b] = true;
               stack[top++] = b;
            }
         }
      }
      return Arrays.copyOf(out, count);
   }

   int size() {
      return nodeX.length;
   }
}
