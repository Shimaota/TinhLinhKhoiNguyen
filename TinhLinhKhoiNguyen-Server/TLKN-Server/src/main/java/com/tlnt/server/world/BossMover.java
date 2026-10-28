package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;

import java.util.Random;

/**
 * Cách đi của boss do server điều khiển ({@link AltarBoss}: boss Tế đàn, boss thế giới người chơi giả, Tiểu Tinh linh
 * Cáo) — như người chơi / thú cưng trên địa hình 2D, KHÔNG bay:
 * <ul>
 *   <li>Đường đi tìm trên {@link NavGrid} (mặt đứng được theo cột): chạy ngang trên mặt đất, rơi khỏi mép, rơi xuyên nền
 *       1 chiều, nhảy lên bậc / tầng cao hơn (≤ {@link NavGrid#MAX_JUMP_PX}) theo trọng lực {@link #GRAVITY}.</li>
 *   <li>Trên không vẫn lái ngang được (như người chơi); gặp tường thì dừng / nhảy lên nếu thấp.</li>
 *   <li>Không có đường tới đích: chạy ngang trên tầng đang đứng về phía đích (không bao giờ bay xuyên địa hình).</li>
 * </ul>
 * Client chỉ nội suy theo op 5 server gửi. CHỈ luồng tick của khu đụng tới.
 */
final class BossMover {

   /** Dừng cách mục tiêu bấy nhiêu (trong tầm cận chiến của boss). */
   static final float STOP_PX = 150f;
   /** Chênh mặt đất dưới 2 bên trong mức này = cùng tầng. */
   static final float SAME_LEVEL_PX = 60f;
   static final float STEP_PX = Escort.STEP_PX;
   /** Trọng lực như thú cưng game gốc (đo: 1,5 px / tick² ở 60 Hz ≈ 5400 px/s²) — nhảy nhanh, gọn, không lơ lửng. */
   static final float GRAVITY = 5400f;
   static final float MAX_JUMP_PX = NavGrid.MAX_JUMP_PX;
   /** Nhảy cao hơn mặt cần lên bấy nhiêu (tích phân rời rạc tick 33 ms hụt một chút). */
   static final float JUMP_MARGIN_PX = 70f;
   /** Tính lại đường đi tối đa mỗi bấy lâu (đích đổi thì tính ngay). */
   static final long REPLAN_MS = 800;

   float x;
   float y;
   /** Vị trí đổi từ lần gửi op 5 trước. */
   boolean dirty;

   private float vy;
   private boolean onGround = true;
   private float dropThroughY = Float.NaN;
   /** Đang nhảy / rơi theo 1 cạnh đường đi: lái ngang về đây. */
   private float airTargetX = Float.NaN;
   private int[] path;
   private int pathGoal = -1;
   private long replanAt;
   /**
    * Boss thế giới chạy VÒNG sang phía này của mục tiêu (-1 = bên trái, 1 = bên phải mục tiêu), 0 = đứng phía gần như
    * thường. Tới nơi thì về 0.
    */
   private int forcedSide;

   BossMover(float x, float y) {
      this.x = x;
      this.y = y;
   }

   boolean onGround() {
      return onGround;
   }

   /** Chạy vòng sang phía {@code side} (-1 trái / 1 phải) của mục tiêu — chạy qua chạy lại thay vì đứng 1 chỗ. */
   void crossTo(int side) {
      forcedSide = Integer.signum(side);
   }

   /** Đang chạy vòng sang phía bên kia mục tiêu (chưa tới nơi). */
   boolean crossing() {
      return forcedSide != 0;
   }

   /** Đứng yên (rơi nếu đang trên không). Dùng khi boss đang ra chiêu / bị choáng. */
   void idle(MapTemplate map, long now, float dt) {
      if (!onGround) {
         dt = Math.min(0.1f, dt);
         if (dt > 0) {
            fall(map, dt);
         }
      }
   }

   /** Đuổi {@code target}: tới chỗ đứng cách {@link #STOP_PX} (phía gần, hoặc phía {@link #crossTo}) cùng tầng. */
   void update(WorldPlayer target, MapTemplate map, float speed, long now, float dt) {
      float targetGround = groundUnder(map, target.x, target.y);
      float side = forcedSide != 0 ? forcedSide : target.x >= x ? -1f : 1f;
      float standX = map.clampX(target.x + side * STOP_PX);
      float standY = groundUnder(map, standX, targetGround);
      if (Math.abs(standY - targetGround) > SAME_LEVEL_PX) {
         standX = target.x;
         standY = targetGround;
         forcedSide = 0; // phía bên kia không đứng được (mép / bậc): thôi chạy vòng
      }
      if (forcedSide != 0 && onGround && Math.abs(standX - x) < 30f) {
         forcedSide = 0; // đã sang tới nơi
      }
      navigate(map, standX, standY, speed, now, dt);
   }

   /** Đi tới chỗ ({@code tx}, mặt đất gần {@code ty}) — đi tuần / chạy trốn / về chỗ cũ. */
   void walkTo(MapTemplate map, float tx, float ty, float speed, long now, float dt) {
      navigate(map, tx, groundUnder(map, tx, ty), speed, now, dt);
   }

   /** Đã đứng (trên đất) cách ({@code tx}, {@code ty}) trong {@code px}. */
   boolean arrived(float tx, float ty, float px) {
      return onGround && Math.abs(tx - x) <= px && Math.abs(ty - y) <= SAME_LEVEL_PX;
   }

   /**
    * 1 bước đi theo đường mặt đất tới ({@code gx}, {@code gy}) với tốc chạy {@code speed} px / s trong {@code dt} giây.
    */
   void navigate(MapTemplate map, float gx, float gy, float speed, long now, float dt) {
      dt = Math.min(0.1f, dt);
      if (dt <= 0) {
         return;
      }
      if (!onGround) { // đang nhảy / rơi: lái ngang về đích của cạnh đang đi
         steer(map, Float.isNaN(airTargetX) ? gx : airTargetX, speed, dt, false);
         fall(map, dt);
         return;
      }
      airTargetX = Float.NaN;
      NavGrid nav = NavGrid.of(map);
      int cur = nav.nodeAt(map, x, y);
      int goal = nav.nodeAt(map, gx, gy);
      int at = indexOf(cur);
      if (goal != pathGoal || at < 0 || now >= replanAt) {
         path = nav.path(cur, goal);
         pathGoal = goal;
         replanAt = now + REPLAN_MS;
         at = indexOf(cur);
      }
      float moveX = gx;
      if (path != null && at >= 0 && at < path.length - 1) {
         int next = path[at + 1];
         switch (nav.kind(cur, next)) {
            case NavGrid.DROP -> { // rơi xuyên nền 1 chiều
               dropThroughY = y;
               onGround = false;
               vy = 0;
               airTargetX = nav.nodeX[next];
               fall(map, dt);
               return;
            }
            case NavGrid.JUMP -> {
               float takeoff = nav.nodeX[cur];
               if (Math.abs(x - takeoff) <= NavGrid.COL_PX / 2) {
                  jump(nav.nodeY[next] - y + JUMP_MARGIN_PX);
                  airTargetX = nav.nodeX[next];
                  steer(map, airTargetX, speed, dt, false);
                  fall(map, dt);
                  return;
               }
               moveX = takeoff; // tới chỗ cất cánh trước
            }
            default -> moveX = nav.nodeX[next]; // đi bộ / chạy ra mép rơi xuống
         }
      }
      steer(map, moveX, speed, dt, true);
      settle(map, dt);
   }

   /** Vị trí trong đường đi hiện tại của nút {@code node}; -1 = không có. */
   private int indexOf(int node) {
      if (path == null || node < 0) {
         return -1;
      }
      for (int i = 0; i < path.length; i++) {
         if (path[i] == node) {
            return i;
         }
      }
      return -1;
   }

   /** Chạy ngang về {@code tx}; gặp tường: đang đứng đất + tường thấp ({@code climb}) thì nhảy lên, không thì dừng. */
   private void steer(MapTemplate map, float tx, float speed, float dt, boolean climb) {
      float gap = map.clampX(tx) - x;
      if (Math.abs(gap) <= 1f) {
         return;
      }
      float nx = map.clampX(x + Math.signum(gap) * Math.min(Math.abs(gap), speed * dt));
      float wall = Escort.wallTop(map, nx, y);
      if (Float.isNaN(wall)) {
         x = nx;
         dirty = true;
      } else if (climb && onGround && wall - y <= MAX_JUMP_PX) {
         jump(wall - y + JUMP_MARGIN_PX);
         airTargetX = nx;
      }
   }

   /** Đứng trên mặt đất (bám bậc thấp / dốc) hoặc bắt đầu rơi nếu đã ra mép. */
   private void settle(MapTemplate map, float dt) {
      if (onGround) {
         float ground = Pet.surfaceBelow(map, x, y + STEP_PX);
         if (Float.isNaN(ground) || y - ground > STEP_PX) {
            onGround = false; // đi ra mép
            vy = 0;
         } else if (ground != y) {
            y = ground;
            dirty = true;
         }
      }
      if (!onGround) {
         fall(map, dt);
      }
   }

   private void fall(MapTemplate map, float dt) {
      vy -= GRAVITY * dt;
      float ny = y + vy * dt;
      if (vy <= 0) {
         float limit = Float.isNaN(dropThroughY) ? y + 1f : Math.min(y + 1f, dropThroughY - 1f);
         float ground = Pet.surfaceBelow(map, x, limit);
         if (!Float.isNaN(ground) && ny <= ground) {
            ny = ground;
            vy = 0;
            onGround = true;
            dropThroughY = Float.NaN;
            airTargetX = Float.NaN;
         }
      }
      if (ny < -500) { // rơi khỏi map (lỗi dữ liệu va chạm): đứng lại chỗ có đất gần nhất phía trên
         float ground = Pet.surfaceBelow(map, x, map.height());
         ny = Float.isNaN(ground) ? map.spawnY() : ground;
         vy = 0;
         onGround = true;
      }
      y = ny;
      dirty = true;
   }

   /** Nhảy tại chỗ cao {@code height} px nếu đang đứng đất (nhân vật treo offline nhảy lên đánh quái bay). */
   boolean hop(float height) {
      if (!onGround) {
         return false;
      }
      jump(height);
      return true;
   }

   /** Đang ở đỉnh / bắt đầu rơi: nhảy thêm 1 lần giữa không trung (nhảy kép như client {@code autoJump}). */
   boolean hopAgain(float height) {
      if (onGround || vy > 0) {
         return false;
      }
      vy = (float) Math.sqrt(2 * GRAVITY * Math.max(40f, height));
      dirty = true;
      return true;
   }

   /** Vận tốc ngang khi bị boss đẩy văng (px / s), 0 = không — chỉ dùng trong {@link #flyKnocked}. */
   private float knockVx;

   /**
    * Bị đẩy / hất tung (người không có client — bot, treo offline: {@code ZoneCombat.knock}): bay với vận tốc
    * ({@code vx}, {@code vy}) px / s rồi rơi theo trọng lực như client tự bay theo op 12 sub 2.
    */
   void launch(float vx, float vy) {
      knockVx = vx;
      this.vy = vy;
      onGround = false;
      dropThroughY = Float.NaN;
      airTargetX = Float.NaN;
      path = null;
      dirty = true;
   }

   /** 1 bước bay khi bị đẩy: trượt ngang (dừng ở tường) + rơi; chạm đất thì hết. */
   void flyKnocked(MapTemplate map, float dt) {
      dt = Math.min(0.1f, dt);
      if (dt <= 0) {
         return;
      }
      if (knockVx != 0) {
         float nx = map.clampX(x + knockVx * dt);
         if (Float.isNaN(Escort.wallTop(map, nx, y))) {
            x = nx;
            dirty = true;
         } else {
            knockVx = 0; // đập tường
         }
      }
      if (!onGround) {
         fall(map, dt);
      }
      if (onGround) {
         knockVx = 0;
      }
   }

   private void jump(float height) {
      vy = (float) Math.sqrt(2 * GRAVITY * Math.max(40f, height));
      onGround = false;
      dirty = true;
   }

   /** Chỗ đứng ngẫu nhiên đi bộ tới được từ chỗ đang đứng (cả map) — {x, y}; null = không có. */
   float[] randomSpot(MapTemplate map, Random random) {
      NavGrid nav = NavGrid.of(map);
      int[] all = nav.reachable(nav.nodeAt(map, x, y));
      if (all.length == 0) {
         return null;
      }
      int n = all[random.nextInt(all.length)];
      return new float[] {nav.nodeX[n], nav.nodeY[n]};
   }

   /**
    * Chỗ trốn: đi bộ tới được, XA người đuổi ({@code fromX}, {@code fromY}) nhất trong vài chỗ ngẫu nhiên — ưu tiên phía
    * bên kia so với người đuổi. null = không có.
    */
   float[] escapeSpot(MapTemplate map, float fromX, float fromY, Random random) {
      NavGrid nav = NavGrid.of(map);
      int[] all = nav.reachable(nav.nodeAt(map, x, y));
      float[] best = null;
      float bestScore = -Float.MAX_VALUE;
      for (int k = 0; k < Math.min(24, all.length); k++) {
         int n = all[random.nextInt(all.length)];
         float far = Math.abs(nav.nodeX[n] - fromX) + Math.abs(nav.nodeY[n] - fromY) * 0.5f;
         boolean behind = Math.signum(nav.nodeX[n] - x) != Math.signum(fromX - x);
         float score = far + (behind ? 500f : 0f);
         if (score > bestScore) {
            bestScore = score;
            best = new float[] {nav.nodeX[n], nav.nodeY[n]};
         }
      }
      return best;
   }

   /** Mặt đất dưới (px, py); không có thì giữ py. */
   static float groundUnder(MapTemplate map, float px, float py) {
      float g = Pet.surfaceBelow(map, px, py + STEP_PX);
      return Float.isNaN(g) ? py : g;
   }
}
