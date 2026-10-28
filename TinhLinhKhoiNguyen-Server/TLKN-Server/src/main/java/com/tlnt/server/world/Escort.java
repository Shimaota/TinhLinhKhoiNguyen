package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;

import java.util.List;

/**
 * Sinh vật được hộ tống (bước nhiệm vụ có {@code QuestStep.escortItem}, vd 9.4 Moara): 1 người chơi giả do SERVER điều
 * khiển, đi y như thú cưng (port logic client {@code PetBrain}): LUÔN Ở DƯỚI ĐẤT (trọng lực, đứng trên hộp va chạm / nền
 * 1 chiều / dốc — chủ nhảy thì nó không bay theo), lẽo đẽo sau chủ, gặp bậc / tường ≤ {@link #MAX_JUMP_PX} thì nhảy lên
 * (nhảy / rơi nhanh như thú cưng), chủ ở tầng thấp hơn thì rơi xuyên nền 1 chiều hoặc chạy ra mép rơi xuống. Khác thú
 * cưng: tốc ĐI NGANG tối đa {@link #SPEED_FACTOR} × tốc chạy của chủ (chủ chạy liền một mạch thì nó tụt lại), không đi
 * dạo, bị bỏ xa KHÔNG hiện lại cạnh chủ mà {@code Zone.tickEscorts} tính thất bại theo {@link #lag}. Kẹt / bậc cao quá
 * không nhảy tới thì vẫn hiện lại cạnh chủ (lỗi địa hình, không phải lỗi người chơi).
 *
 * <p>Cả khu cùng thấy (op 3 / 5 / 4 phát cho khu, người vào khu sau nhận op 3 ở {@code Zone.join}); thú cưng thường
 * của chủ vẫn đi theo như cũ, không liên quan. Id thực thể {@code WorldPlayer.escortId} nằm ngoài vùng id thú cưng của
 * client ({@code ClickMoveController.PET_ID_BASE}) -> client nội suy op 5 như người chơi khác. CHỈ luồng tick của khu
 * chứa chủ đụng tới.
 */
final class Escort {

   /** Tốc đi ngang tối đa = bấy nhiêu × tốc chạy của chủ. */
   static final float SPEED_FACTOR = 0.5f;
   /** Tụt lại (khoảng cách tới chủ) quá bấy nhiêu: báo cho chủ chờ. */
   static final float WARN_PX = 600f;
   /** Tụt lại quá bấy nhiêu: hộ tống thất bại. */
   static final float FAIL_PX = 1000f;

   static final float GRAVITY = 2800f;
   static final float MAX_JUMP_PX = 650f;
   static final float STEP_PX = 24f;
   static final float BODY_PX = 80f;
   static final float FOLLOW_START_PX = 260f;
   static final float FOLLOW_STOP_PX = 130f;
   static final float ARRIVE_PX = 14f;
   /** Tốc đuổi = khoảng cách còn lại × hệ số này, kẹp trong [tốc thấp nhất, tốc tối đa]. */
   static final float FOLLOW_GAIN = 4f;
   /** Tốc thấp nhất khi đang đi (chậm quá thì animation chạy trông như trượt) — không vượt tốc tối đa. */
   static final float MIN_SPEED = 200f;
   static final long OWNER_MOVING_MS = 300;
   static final float OWNER_DIR_SPEED = 60f;
   static final long CLIMB_AFTER_MS = 700;
   static final long CLIMB_COOLDOWN_MS = 400;
   /**
    * Nhảy cao hơn mặt cần lên bấy nhiêu. Lớn hơn client (20-30) vì tick server 33 ms: tích phân rời rạc hụt ~v·dt/2
    * (~25 px với cú nhảy 300 px) -> biên nhỏ thì đỉnh nhảy thấp hơn mặt bậc, rơi xuống rồi nhảy lại.
    */
   static final float JUMP_MARGIN_PX = 60f;
   static final long DESCEND_AFTER_MS = 350;
   static final long DESCEND_WARP_MS = 3_000;
   static final long STUCK_WARP_MS = 2_500;

   final int templateId;
   final String name;
   /** Các map được đi cùng (rỗng = mọi map) — chủ sang map khác = thất bại ({@code Zone.syncEscort}). */
   final List<Integer> maps;

   float x;
   float y;
   boolean facingLeft;
   /** Đã báo "tụt lại" (tắt khi quay về dưới nửa {@link #WARN_PX}). */
   boolean warned;
   /** Vị trí đổi từ lần gửi op 5 trước. */
   boolean dirty;
   long nextSendAt;
   long lastTickAt;
   /** Khoảng cách tới chủ (ngang + chênh mặt đất) ở tick gần nhất — đổi map: quá {@link #WARN_PX} = bỏ lại phía sau. */
   float lag;

   private float vy;
   private boolean onGround = true;
   private boolean following;
   private long stuckSince;
   private long lastClimbAt;
   private long higherSince;
   private long lowerSince;
   private float dropThroughY = Float.NaN;
   private float ownerVx;
   private float ownerLastX = Float.NaN;

   Escort(int templateId, String name, List<Integer> maps) {
      this.templateId = templateId;
      this.name = name;
      this.maps = maps;
   }

   boolean allowedOn(int mapId) {
      return maps.isEmpty() || maps.contains(mapId);
   }

   /** Hiện ngay sau lưng chủ, trên mặt đất dưới chân chủ (bắt đầu / vào khu mới / kẹt). */
   void placeBeside(WorldPlayer owner, MapTemplate map, long now) {
      facingLeft = owner.facingLeft;
      x = map.clampX(owner.facingLeft ? owner.x + FOLLOW_STOP_PX : owner.x - FOLLOW_STOP_PX);
      float ground = surfaceBelow(map, x, owner.y + STEP_PX);
      y = Float.isNaN(ground) || owner.y - ground > MAX_JUMP_PX ? owner.y : ground;
      vy = 0;
      onGround = true;
      following = false;
      stuckSince = 0;
      higherSince = 0;
      lowerSince = 0;
      dropThroughY = Float.NaN;
      ownerLastX = Float.NaN;
      lag = 0;
      warned = false;
      dirty = true;
      lastTickAt = now;
   }

   /**
    * 1 bước mô phỏng ({@code dt} giây, kẹp ≤ 0,1) với tốc đi ngang tối đa {@code maxSpeed} px / s. Cập nhật
    * {@link #lag} và {@link #dirty}.
    */
   void update(WorldPlayer owner, MapTemplate map, float maxSpeed, long now, float dt) {
      dt = Math.min(0.1f, dt);
      if (dt <= 0) {
         return;
      }
      trackOwner(owner, dt);
      float minSpeed = Math.min(MIN_SPEED, maxSpeed);

      float ownerGround = surfaceBelow(map, owner.x, owner.y + STEP_PX);
      if (Float.isNaN(ownerGround) || owner.y - ownerGround > MAX_JUMP_PX * 2) {
         ownerGround = owner.y;
      }
      float dx = owner.x - x;

      // ---- chọn đích ngang + tốc độ
      float target = Float.NaN;
      float speed = maxSpeed;
      boolean ownerMoving = now - owner.lastMoveAt < OWNER_MOVING_MS;
      int ownerDir = ownerMoving ? ownerDirection() : 0;
      boolean ahead = ownerDir != 0 && Math.signum(x - owner.x) == ownerDir;
      if (Math.abs(dx) > FOLLOW_START_PX || (ownerDir != 0 && !ahead && Math.abs(dx) > FOLLOW_STOP_PX + 40f)) {
         following = true;
      }
      if (following && !(ahead && onGround && Math.abs(ownerGround - y) < 60f)) { // chủ quay đầu: đứng chờ chủ đi qua
         target = followSpot(owner, map, ownerGround, ownerDir);
         float gap = Math.abs(target - x);
         speed = Math.max(minSpeed, Math.min(maxSpeed, gap * FOLLOW_GAIN));
         if (gap < ARRIVE_PX && Math.abs(ownerGround - y) < 60f) {
            target = Float.NaN;
            if (!ownerMoving) {
               following = false;
            }
         }
      }

      // ---- chủ ở tầng cao hơn: đi tới chỗ chủ, nhảy lên khi đụng bậc / tường (đoạn "đi ngang") hoặc khi ngay trên đầu
      // là nền 1 chiều của tầng chủ — KHÔNG nhảy tại chỗ lúc còn ở xa (khác PetBrain: đi chậm thì nhảy đoán lộ rõ)
      float rise = ownerGround - y;
      if (onGround && rise > 60f && owner.y - ownerGround < 5f) {
         following = true;
         target = owner.x;
         speed = maxSpeed;
         if (higherSince == 0) {
            higherSince = now;
         }
         if (rise > MAX_JUMP_PX) {
            if (now - higherSince > CLIMB_AFTER_MS) { // cao quá không nhảy tới: hiện lại cạnh chủ
               placeBeside(owner, map, now);
               return;
            }
         } else if (platformAbove(map, x, ownerGround) && now - lastClimbAt > CLIMB_COOLDOWN_MS) {
            lastClimbAt = now;
            jump(rise + JUMP_MARGIN_PX);
         }
      } else if (onGround && rise <= 60f) {
         higherSince = 0;
      }

      // ---- chủ ở tầng thấp hơn: rơi xuyên nền 1 chiều, hoặc chạy ra mép rơi xuống
      float drop = y - ownerGround;
      if (onGround && drop > 60f && owner.y - ownerGround < 5f) {
         if (lowerSince == 0) {
            lowerSince = now;
         }
         if (now - lowerSince > DESCEND_AFTER_MS) {
            following = true;
            if (standingOnOneWay(map, x, y)) {
               dropThroughY = y;
               onGround = false;
               vy = 0;
            } else if (now - lowerSince > DESCEND_WARP_MS) {
               placeBeside(owner, map, now);
               return;
            } else {
               target = owner.x;
               speed = maxSpeed;
            }
         }
      } else if (onGround && drop <= 60f) {
         lowerSince = 0;
      }

      float oldX = x;
      float oldY = y;

      // ---- đi ngang
      if (!Float.isNaN(target) && Math.abs(target - x) > 0.5f) {
         float dir = Math.signum(target - x);
         float remaining = Math.abs(target - x);
         float step = speed * dt;
         if (remaining - step < minSpeed * dt) {
            step = remaining;
         }
         float nx = map.clampX(x + dir * step);
         facingLeft = dir < 0;
         float wall = wallTop(map, nx, y);
         if (!Float.isNaN(wall)) {
            float h = wall - y;
            if (onGround && h <= MAX_JUMP_PX) {
               jump(h + JUMP_MARGIN_PX);
            }
         } else {
            x = nx;
         }
      }

      // ---- đứng đất / rơi / nhảy
      if (onGround) {
         float ground = surfaceBelow(map, x, y + STEP_PX);
         if (Float.isNaN(ground) || y - ground > STEP_PX) {
            onGround = false;
            vy = 0;
         } else {
            y = ground;
         }
      }
      if (!onGround) {
         vy -= GRAVITY * dt;
         float ny = y + vy * dt;
         if (vy <= 0) {
            float limit = Float.isNaN(dropThroughY) ? y + 1f : Math.min(y + 1f, dropThroughY - 1f);
            float ground = surfaceBelow(map, x, limit);
            if (!Float.isNaN(ground) && ny <= ground) {
               ny = ground;
               vy = 0;
               onGround = true;
               dropThroughY = Float.NaN;
            }
         }
         if (ny < -500) { // rơi khỏi map
            placeBeside(owner, map, now);
            return;
         }
         y = ny;
      }

      // ---- kẹt
      if (following && !Float.isNaN(target) && x == oldX && onGround) {
         if (stuckSince == 0) {
            stuckSince = now;
         } else if (now - stuckSince > STUCK_WARP_MS) {
            placeBeside(owner, map, now);
            return;
         }
      } else {
         stuckSince = 0;
      }
      if (x != oldX || y != oldY) {
         dirty = true;
      }
      lag = (float) Math.hypot(owner.x - x, ownerGround - y);
   }

   /** Chỗ đứng khi đi theo: cách chủ {@link #FOLLOW_STOP_PX}, sau lưng nếu chủ đang đi, phải cùng tầng với chủ. */
   private float followSpot(WorldPlayer owner, MapTemplate map, float ownerGround, int ownerDir) {
      float side = ownerDir != 0 ? -ownerDir : x < owner.x ? -1f : 1f;
      for (int k = 0; k < 2; k++) {
         float spot = map.clampX(owner.x + side * FOLLOW_STOP_PX);
         if (sameLevel(map, spot, ownerGround)) {
            return spot;
         }
         side = -side;
      }
      return owner.x;
   }

   private void jump(float height) {
      vy = (float) Math.sqrt(2 * GRAVITY * Math.max(40f, height));
      onGround = false;
   }

   private void trackOwner(WorldPlayer owner, float dt) {
      if (!Float.isNaN(ownerLastX)) {
         float v = Math.max(-1_000f, Math.min(1_000f, (owner.x - ownerLastX) / dt));
         ownerVx = ownerVx * 0.8f + v * 0.2f;
      }
      ownerLastX = owner.x;
   }

   private int ownerDirection() {
      return ownerVx > OWNER_DIR_SPEED ? 1 : ownerVx < -OWNER_DIR_SPEED ? -1 : 0;
   }

   // ------------------------------------------------------------------ địa hình (giống client PetBrain)

   static float surfaceBelow(MapTemplate map, float x, float maxY) {
      return Pet.surfaceBelow(map, x, maxY);
   }

   /** Ngay trên đầu (tại {@code x}) có mặt đứng ở độ cao {@code level} (nền tầng của chủ) -> nhảy lên được. */
   private static boolean platformAbove(MapTemplate map, float x, float level) {
      float top = surfaceBelow(map, x, level + STEP_PX);
      return !Float.isNaN(top) && Math.abs(top - level) <= STEP_PX;
   }

   private static boolean sameLevel(MapTemplate map, float x, float ground) {
      float here = surfaceBelow(map, x, ground + STEP_PX);
      return !Float.isNaN(here) && Math.abs(here - ground) <= STEP_PX && Float.isNaN(wallTop(map, x, ground));
   }

   /** Tường / bậc chắn khi bước tới {@code x} (hộp đặc cao hơn chân quá {@link #STEP_PX}), NaN = đi được. */
   static float wallTop(MapTemplate map, float x, float feet) {
      float best = Float.NaN;
      for (MapTemplate.Collider c : map.colliders()) {
         if (c.triangle() || c.oneWay() || x < c.x() || x > c.x() + c.w()) {
            continue;
         }
         float top = c.y() + c.h();
         if (top > feet + STEP_PX && c.y() < feet + BODY_PX && (Float.isNaN(best) || top > best)) {
            best = top;
         }
      }
      return best;
   }

   /** Mặt đang đứng chỉ toàn nền 1 chiều (rơi xuyên qua được). */
   static boolean standingOnOneWay(MapTemplate map, float x, float feet) {
      boolean any = false;
      for (MapTemplate.Collider c : map.colliders()) {
         float top;
         if (c.triangle()) {
            top = Pet.triangleTop(c.points(), x);
         } else if (x >= c.x() && x <= c.x() + c.w()) {
            top = c.y() + c.h();
         } else {
            continue;
         }
         if (!Float.isNaN(top) && Math.abs(top - feet) < 2f) {
            if (!c.oneWay()) {
               return false;
            }
            any = true;
         }
      }
      return any;
   }
}
