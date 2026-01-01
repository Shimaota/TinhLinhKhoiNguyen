package com.tlnt.world.entity.player.pet;

import java.util.Random;

/**
 * A pet following a player — simulated by EACH CLIENT (the server used to simulate it and broadcast op 5 to the whole zone; now the
 * server only says which pet goes with whom via op 3 / op 4, costing no movement packets). Ported from the server's
 * {@code world/Pet.java}, in PIXELS. Different machines may see the pet slightly differently (random wandering) — purely
 * visual, pets cannot be targeted.
 *
 * <ul>
 *   <li>Always on the ground: gravity applies, it stands on collision boxes (one-way platforms too) and slopes; when the owner jumps the pet does not
 *       fly along.</li>
 *   <li>Trails behind its owner: speed eases with distance ({@link #FOLLOW_GAIN}), capped at the owner's speed.</li>
 *   <li>Steps / walls up to {@link #MAX_JUMP_PX} high are jumped; if the owner stands on a higher floor it jumps up after them. Too high
 *       / stuck too long / left too far behind -> it reappears right next to the owner.</li>
 *   <li>When the owner stands still the pet wanders back and forth around them (without falling off edges).</li>
 * </ul>
 */
public final class PetBrain {

   /** Result of one simulation step. */
   public static final int STILL = 0;
   public static final int MOVED = 1;
   public static final int WARPED = 2;

   /** Owner position / state (px) — updated by the caller every frame. */
   public static final class Owner {
      public float x;
      public float y;
      public boolean facingLeft;
      /** Last time the owner's position changed (ms). */
      public long lastMoveAt;
   }

   static final float RUN_SPEED = 300f;
   /** Minimum speed while moving (slowing down while chasing, patrolling) — too slow and the run animation looks like sliding. */
   static final float SMOOTH_MIN_SPEED = 300f;
   static final float MIN_FOLLOW_SPEED = SMOOTH_MIN_SPEED;
   /** Chase speed = remaining distance × this factor (px/s per px), clamped to [MIN_FOLLOW_SPEED, owner speed]. */
   static final float FOLLOW_GAIN = 4f;
   /** The owner moved within this long = walking -> the pet does not fully stop (avoids run - stop - run). */
   static final long OWNER_MOVING_MS = 300;
   /** The owner must move faster than this (px/s) to count as walking in one direction. */
   static final float OWNER_DIR_SPEED = 60f;
   static final float WANDER_SPEED = SMOOTH_MIN_SPEED;
   static final float WANDER_MIN_LEG_PX = 70f;
   /** Probability that a patrol leg continues on the same side (no turning around). */
   static final float WANDER_SAME_SIDE_CHANCE = 0.25f;
   static final int WANDER_PAUSE_MIN_MS = 5_000;
   static final int WANDER_PAUSE_MAX_MS = 10_000;
   static final float GRAVITY = 2800f;
   static final float MAX_JUMP_PX = 650f;
   static final long CLIMB_AFTER_MS = 700;
   static final int MAX_CLIMB_FAILS = 3;
   static final float STEP_PX = 24f;
   static final float BODY_PX = 80f;
   static final float FOLLOW_START_PX = 260f;
   static final float FOLLOW_STOP_PX = 130f;
   static final float ARRIVE_PX = 14f;
   static final long DESCEND_AFTER_MS = 350;
   static final long DESCEND_WARP_MS = 3_000;
   /** MUST be less than {@link #FOLLOW_START_PX}. */
   static final float WANDER_RANGE_PX = 170f;
   static final long IDLE_BEFORE_WANDER_MS = 2_500;
   static final float WARP_PX = 1_000f;
   static final long STUCK_WARP_MS = 2_500;
   private static final Random RANDOM = new Random();

   public float x;
   public float y;
   public boolean facingLeft;
   private float vy;
   private boolean onGround = true;
   private boolean following;
   private float wanderX = Float.NaN;
   private long nextWanderAt;
   private long stuckSince;
   private long lastClimbAt;
   private long higherSince;
   private int climbFails;
   private long lowerSince;
   private float dropThroughY = Float.NaN;
   private float ownerSpeed;
   private float ownerVx;
   private float ownerLastX = Float.NaN;

   /** Place right behind the owner, on the ground under the owner's feet. */
   public void placeBeside(Owner owner, PetTerrain map) {
      this.facingLeft = owner.facingLeft;
      this.x = map.clampX(owner.facingLeft ? owner.x + FOLLOW_STOP_PX : owner.x - FOLLOW_STOP_PX);
      float ground = surfaceBelow(map, this.x, owner.y + STEP_PX);
      this.y = Float.isNaN(ground) || owner.y - ground > MAX_JUMP_PX ? owner.y : ground;
      this.vy = 0;
      this.onGround = true;
      this.following = false;
      this.wanderX = Float.NaN;
      this.stuckSince = 0;
      this.higherSince = 0;
      this.climbFails = 0;
      this.lowerSince = 0;
      this.dropThroughY = Float.NaN;
   }

   /**
    * One simulation step.
    * @param now system time (ms) — timers (wandering, stuck...)
    * @param dt  step duration (seconds), clamped to ≤ 0.1
    * @return {@link #STILL} / {@link #MOVED} / {@link #WARPED} (reappeared next to the owner)
    */
   public int update(Owner owner, PetTerrain map, long now, float dt) {
      dt = Math.min(0.1f, dt);
      if (dt <= 0) {
         return STILL;
      }
      trackOwnerSpeed(owner, dt);

      float ownerGround = surfaceBelow(map, owner.x, owner.y + STEP_PX);
      if (Float.isNaN(ownerGround) || owner.y - ownerGround > MAX_JUMP_PX * 2) {
         ownerGround = owner.y;
      }
      float dx = owner.x - this.x;
      if (Math.abs(dx) > WARP_PX || Math.abs(ownerGround - this.y) > WARP_PX || this.y < -500) {
         placeBeside(owner, map);
         return WARPED;
      }

      // ---- choose horizontal goal + speed
      float target = Float.NaN;
      float speed = RUN_SPEED;
      boolean wandering = false;
      boolean ownerMoving = now - owner.lastMoveAt < OWNER_MOVING_MS;
      int ownerDir = ownerMoving ? ownerDirection() : 0;
      boolean petAhead = ownerDir != 0 && Math.signum(this.x - owner.x) == ownerDir;
      if (Math.abs(dx) > FOLLOW_START_PX || (ownerDir != 0 && !petAhead && Math.abs(dx) > FOLLOW_STOP_PX + 40f)) {
         this.following = true;
      }
      if (this.following && petAhead && this.onGround && Math.abs(ownerGround - this.y) < 60f) {
         // The owner turned around towards the pet: wait for the owner to pass, then follow behind (instead of running into them).
         this.wanderX = Float.NaN;
      } else if (this.following) {
         this.wanderX = Float.NaN;
         target = followSpot(owner, map, ownerGround, ownerDir);
         float gap = Math.abs(target - this.x);
         speed = Math.max(MIN_FOLLOW_SPEED, Math.min(Math.max(RUN_SPEED, this.ownerSpeed), gap * FOLLOW_GAIN));
         if (gap < ARRIVE_PX && Math.abs(ownerGround - this.y) < 60f) {
            target = Float.NaN;
            if (!ownerMoving) {
               this.following = false;
               this.nextWanderAt = now + 800;
            }
         }
      } else if (this.onGround && now - owner.lastMoveAt > IDLE_BEFORE_WANDER_MS) {
         if (Float.isNaN(this.wanderX) && now >= this.nextWanderAt) {
            this.wanderX = pickWander(owner, map);
            if (Float.isNaN(this.wanderX)) {
               this.nextWanderAt = now + 1_000;
            }
         }
         if (!Float.isNaN(this.wanderX)) {
            target = this.wanderX;
            speed = WANDER_SPEED;
            wandering = true;
         }
      } else {
         this.wanderX = Float.NaN;
      }

      // ---- owner on a higher floor: jump up after them
      float rise = ownerGround - this.y;
      if (this.onGround && rise > 60f && owner.y - ownerGround < 5f) {
         if (this.higherSince == 0) {
            this.higherSince = now;
         }
         boolean near = Math.abs(dx) < FOLLOW_START_PX + 60f;
         if ((near || now - this.higherSince > CLIMB_AFTER_MS) && now - this.lastClimbAt > 1_200) {
            if (rise > MAX_JUMP_PX || this.climbFails >= MAX_CLIMB_FAILS) {
               placeBeside(owner, map);
               return WARPED;
            }
            this.lastClimbAt = now;
            this.climbFails++;
            jump(rise + 30f);
            this.following = true;
            target = owner.x;
            speed = Math.max(RUN_SPEED, this.ownerSpeed);
         }
      } else if (this.onGround && rise <= 60f) {
         this.higherSince = 0;
         this.climbFails = 0;
      }

      // ---- owner on a lower floor: drop through a one-way platform, or run to the edge and fall
      float drop = this.y - ownerGround;
      if (this.onGround && drop > 60f && owner.y - ownerGround < 5f) {
         if (this.lowerSince == 0) {
            this.lowerSince = now;
         }
         if (now - this.lowerSince > DESCEND_AFTER_MS) {
            this.following = true;
            this.wanderX = Float.NaN;
            if (standingOnOneWay(map, this.x, this.y)) {
               this.dropThroughY = this.y;
               this.onGround = false;
               this.vy = 0;
            } else if (now - this.lowerSince > DESCEND_WARP_MS) {
               placeBeside(owner, map);
               return WARPED;
            } else {
               target = owner.x;
               speed = Math.max(RUN_SPEED, this.ownerSpeed);
            }
         }
      } else if (this.onGround && drop <= 60f) {
         this.lowerSince = 0;
      }

      float oldX = this.x;
      float oldY = this.y;

      // ---- horizontal movement
      if (!Float.isNaN(target) && Math.abs(target - this.x) > 0.5f) {
         float dir = Math.signum(target - this.x);
         float remaining = Math.abs(target - this.x);
         float step = speed * dt;
         if (remaining - step < MIN_FOLLOW_SPEED * dt) {
            step = remaining;
         }
         float nx = map.clampX(this.x + dir * step);
         this.facingLeft = dir < 0;
         float wall = wallTop(map, nx, this.y);
         if (!Float.isNaN(wall)) {
            float h = wall - this.y;
            if (this.onGround && h <= MAX_JUMP_PX) {
               jump(h + 20f);
            } else if (wandering) {
               this.wanderX = Float.NaN;
               this.nextWanderAt = now + 1_000;
            }
         } else if (wandering && this.onGround && dropAt(map, nx, this.y) > STEP_PX * 2) {
            this.wanderX = Float.NaN;
            this.nextWanderAt = now + 1_000;
         } else {
            this.x = nx;
         }
         if (wandering && Math.abs(this.wanderX - this.x) < 2f) {
            this.wanderX = Float.NaN;
            this.nextWanderAt = now + WANDER_PAUSE_MIN_MS + RANDOM.nextInt(WANDER_PAUSE_MAX_MS - WANDER_PAUSE_MIN_MS + 1);
         }
      }

      // ---- grounded / falling / jumping
      if (this.onGround) {
         float ground = surfaceBelow(map, this.x, this.y + STEP_PX);
         if (Float.isNaN(ground) || this.y - ground > STEP_PX) {
            this.onGround = false;
            this.vy = 0;
         } else {
            this.y = ground;
         }
      }
      if (!this.onGround) {
         this.vy -= GRAVITY * dt;
         float ny = this.y + this.vy * dt;
         if (this.vy <= 0) {
            float limit = Float.isNaN(this.dropThroughY) ? this.y + 1f : Math.min(this.y + 1f, this.dropThroughY - 1f);
            float ground = surfaceBelow(map, this.x, limit);
            if (!Float.isNaN(ground) && ny <= ground) {
               ny = ground;
               this.vy = 0;
               this.onGround = true;
               this.dropThroughY = Float.NaN;
            }
         }
         this.y = ny;
      }

      // ---- stuck
      if (this.following && !Float.isNaN(target) && this.x == oldX && this.onGround) {
         if (this.stuckSince == 0) {
            this.stuckSince = now;
         } else if (now - this.stuckSince > STUCK_WARP_MS) {
            placeBeside(owner, map);
            return WARPED;
         }
      } else {
         this.stuckSince = 0;
      }
      return this.x != oldX || this.y != oldY ? MOVED : STILL;
   }

   private float pickWander(Owner owner, PetTerrain map) {
      float away = this.x < owner.x ? 1f : -1f;
      for (int k = 0; k < 6; k++) {
         float side = RANDOM.nextFloat() < WANDER_SAME_SIDE_CHANCE ? -away : away;
         float pick = map.clampX(owner.x + side * WANDER_RANGE_PX * RANDOM.nextFloat());
         if (Math.abs(pick - this.x) >= WANDER_MIN_LEG_PX && sameLevel(map, pick, this.y)) {
            return pick;
         }
      }
      return Float.NaN;
   }

   /** Follow position: {@link #FOLLOW_STOP_PX} from the owner, behind them if they are walking, on the same floor as the owner. */
   private float followSpot(Owner owner, PetTerrain map, float ownerGround, int ownerDir) {
      float side = ownerDir != 0 ? -ownerDir : this.x < owner.x ? -1f : 1f;
      for (int k = 0; k < 2; k++) {
         float spot = map.clampX(owner.x + side * FOLLOW_STOP_PX);
         if (sameLevel(map, spot, ownerGround)) {
            return spot;
         }
         side = -side;
      }
      return owner.x;
   }

   private static boolean sameLevel(PetTerrain map, float x, float ground) {
      float here = surfaceBelow(map, x, ground + STEP_PX);
      return !Float.isNaN(here) && Math.abs(here - ground) <= STEP_PX && Float.isNaN(wallTop(map, x, ground));
   }

   private void jump(float height) {
      this.vy = (float) Math.sqrt(2 * GRAVITY * Math.max(40f, height));
      this.onGround = false;
   }

   private void trackOwnerSpeed(Owner owner, float dt) {
      if (!Float.isNaN(this.ownerLastX)) {
         float v = Math.max(-1_000f, Math.min(1_000f, (owner.x - this.ownerLastX) / dt));
         this.ownerSpeed = this.ownerSpeed * 0.8f + Math.abs(v) * 0.2f;
         this.ownerVx = this.ownerVx * 0.8f + v * 0.2f;
      }
      this.ownerLastX = owner.x;
   }

   private int ownerDirection() {
      return this.ownerVx > OWNER_DIR_SPEED ? 1 : this.ownerVx < -OWNER_DIR_SPEED ? -1 : 0;
   }

   // ------------------------------------------------------------------ terrain

   /** Highest standable surface at {@code x} not above {@code maxY} (box top / slope top edge), NaN = none. */
   static float surfaceBelow(PetTerrain map, float x, float maxY) {
      float best = Float.NaN;
      for (int i = 0; i < map.colliders.size(); i++) {
         PetTerrain.Collider c = map.colliders.get(i);
         float top;
         if (c.isTriangle()) {
            top = triangleTop(c.points, x);
         } else if (x >= c.x && x <= c.x + c.w) {
            top = c.y + c.h;
         } else {
            continue;
         }
         if (!Float.isNaN(top) && top <= maxY && (Float.isNaN(best) || top > best)) {
            best = top;
         }
      }
      return best;
   }

   /** Height of the triangle's top edge at {@code x}, NaN if {@code x} is outside. */
   static float triangleTop(float[] p, float x) {
      float top = Float.NaN;
      for (int i = 0; i < 3; i++) {
         float x1 = p[i * 2];
         float y1 = p[i * 2 + 1];
         float x2 = p[(i + 1) % 3 * 2];
         float y2 = p[(i + 1) % 3 * 2 + 1];
         if (x1 == x2 || x < Math.min(x1, x2) || x > Math.max(x1, x2)) {
            continue;
         }
         float yy = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
         if (Float.isNaN(top) || yy > top) {
            top = yy;
         }
      }
      return top;
   }

   /** Wall / step blocking a move to {@code x} (a solid box higher than the feet by more than {@link #STEP_PX}), NaN = passable. */
   static float wallTop(PetTerrain map, float x, float feet) {
      float best = Float.NaN;
      for (int i = 0; i < map.colliders.size(); i++) {
         PetTerrain.Collider c = map.colliders.get(i);
         if (c.isTriangle() || c.oneWay || x < c.x || x > c.x + c.w) {
            continue;
         }
         float top = c.y + c.h;
         if (top > feet + STEP_PX && c.y < feet + BODY_PX && (Float.isNaN(best) || top > best)) {
            best = top;
         }
      }
      return best;
   }

   /** The surface being stood on is made only of one-way platforms (can be dropped through). */
   static boolean standingOnOneWay(PetTerrain map, float x, float feet) {
      boolean any = false;
      for (int i = 0; i < map.colliders.size(); i++) {
         PetTerrain.Collider c = map.colliders.get(i);
         float top;
         if (c.isTriangle()) {
            top = triangleTop(c.points, x);
         } else if (x >= c.x && x <= c.x + c.w) {
            top = c.y + c.h;
         } else {
            continue;
         }
         if (!Float.isNaN(top) && Math.abs(top - feet) < 2f) {
            if (!c.oneWay) {
               return false;
            }
            any = true;
         }
      }
      return any;
   }

   /** How far below the feet the ground is when stepping to {@code x} (no ground = very deep). */
   static float dropAt(PetTerrain map, float x, float feet) {
      float ground = surfaceBelow(map, x, feet + STEP_PX);
      return Float.isNaN(ground) ? Float.MAX_VALUE : feet - ground;
   }
}
