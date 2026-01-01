package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.core.GameState;
import com.tlnt.engine.physics.Direction;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.entity.player.pet.PetBrain;
import com.tlnt.world.entity.player.pet.PetTerrain;

/**
 * Movement of OTHER players / pets from server positions (op 5 sub 0) — delayed interpolation (snapshot
 * interpolation): every received position is stored with a timestamp; each frame the entity is drawn at "now - 100 ms",
 * linearly interpolated between the 2 surrounding snapshots. The server broadcasts ~20 times per second, yet motion stays smooth at any FPS (60 / 144 Hz),
 * velocity is stable (run / idle animations are correct) and it does not depend on packets arriving evenly.
 *
 * Previously: pulled 50% of the remaining distance each frame -> FPS dependent, the server had to broadcast every tick to look smooth.
 *
 * PETS (id ≤ {@link #PET_ID_BASE}, server: {@code WorldPlayer.petId}): receive no positions from the server — each frame they
 * simulate following their owner ({@link PetBrain}, owner = entity id {@code PET_ID_BASE - petId}).
 */
public final class ClickMoveController extends BaseComponent {

   /** Render this many ms in the past: ≥ 2 server broadcast periods (world.moveBroadcastMillis = 50) so there is always a later snapshot. */
   private static final long DELAY_MS = 100L;
   /** Nominal server broadcast period — the first snapshot after standing still is backdated by this, bunched packets are spread out. */
   private static final long NOMINAL_MS = 50L;
   /** Jumps larger than this (world units = px / 100): teleport, zone change... -> snap directly, no sliding across the screen. */
   private static final float SNAP_DISTANCE = 6.0F;
   /** Position changed elsewhere (teleport packet, revive) further than this from the last placement -> drop old snapshots. */
   private static final float EXTERNAL_MOVE = 1.0F;
   private static final int CAPACITY = 16;
   /** Pet id = PET_ID_BASE - owner id (server {@code WorldPlayer.PET_ID_BASE}); clone images -1, -2... do not collide. */
   private static final int PET_ID_BASE = Integer.MIN_VALUE / 2;

   public PlayerEntity projectile;
   private final long[] times = new long[CAPACITY];
   private final float[] xs = new float[CAPACITY];
   private final float[] ys = new float[CAPACITY];
   private int count;
   /** There are snapshots not yet reached (or velocity not yet zeroed after stopping). */
   private boolean moving;
   private final Vector2 applied = new Vector2();
   private final Vector2 target = new Vector2();
   /** Pet: the brain + the id of the entity it controls (pooled entities can change id). */
   private PetBrain pet;
   private int petEntityId;
   private final PetBrain.Owner petOwner = new PetBrain.Owner();
   /** The pet is standing still and velocity is already zero -> do not reset the position every frame (Box2D setTransform is costly). */
   private boolean petResting;

   public ClickMoveController(PlayerEntity player) {
      super(player.getEntity());
      this.projectile = player;
   }

   /** New position from the server (name kept so MessageHandler does not change). */
   public final void setClickTarget(float x, float y) {
      long now = System.currentTimeMillis();
      Vector2 position = this.projectile.getPosition();
      if (!this.moving || this.count == 0) {
         if (position.dst(x, y) > SNAP_DISTANCE) {
            this.snap(x, y);
            return;
         }
         // Starting to move from standing still: snapshot 0 = current position -> no jump to the first snapshot.
         this.count = 0;
         this.push(now - NOMINAL_MS, position.x, position.y);
         this.push(now, x, y);
         this.applied.set(position);
         this.moving = true;
         return;
      }
      int last = this.count - 1;
      if (Math.abs(this.xs[last] - x) + Math.abs(this.ys[last] - y) > SNAP_DISTANCE) {
         this.snap(x, y);
         return;
      }
      // Packets arrived bunched up (network hiccup): spread them at least half a period apart so interpolation does not skip.
      this.push(Math.max(now, this.times[last] + NOMINAL_MS / 2), x, y);
   }

   private void push(long time, float x, float y) {
      if (this.count == CAPACITY) {
         System.arraycopy(this.times, 1, this.times, 0, CAPACITY - 1);
         System.arraycopy(this.xs, 1, this.xs, 0, CAPACITY - 1);
         System.arraycopy(this.ys, 1, this.ys, 0, CAPACITY - 1);
         this.count--;
      }
      this.times[this.count] = time;
      this.xs[this.count] = x;
      this.ys[this.count] = y;
      this.count++;
   }

   private void snap(float x, float y) {
      this.count = 0;
      this.moving = false;
      this.projectile.getEntity().getBodyComponent().getBody().setLinearVelocity(Vector2.Zero);
      this.target.set(x, y);
      this.projectile.setWorldPosition(this.target);
      this.applied.set(x, y);
   }

   @Override
   public final void update(float delta) {
      int id = this.projectile.getId();
      if (id <= PET_ID_BASE) {
         this.updatePet(id, delta);
         return;
      }
      if (!this.moving || this.count == 0) {
         return;
      }
      Vector2 position = this.projectile.getPosition();
      if (position.dst(this.applied) > EXTERNAL_MOVE) { // moved by another packet: follow that position
         this.count = 0;
         this.moving = false;
         return;
      }
      long renderTime = System.currentTimeMillis() - DELAY_MS;
      while (this.count >= 2 && this.times[1] <= renderTime) { // drop snapshots already passed, keep 1 behind
         System.arraycopy(this.times, 1, this.times, 0, this.count - 1);
         System.arraycopy(this.xs, 1, this.xs, 0, this.count - 1);
         System.arraycopy(this.ys, 1, this.ys, 0, this.count - 1);
         this.count--;
      }
      boolean finished = false;
      if (this.count >= 2 && renderTime > this.times[0]) {
         float alpha = (float) (renderTime - this.times[0]) / (float) (this.times[1] - this.times[0]);
         this.target.set(this.xs[0] + (this.xs[1] - this.xs[0]) * alpha, this.ys[0] + (this.ys[1] - this.ys[0]) * alpha);
      } else {
         this.target.set(this.xs[0], this.ys[0]);
         // Last snapshot reached and standing at the right spot: this placement sets velocity 0 (idle animation), then rest.
         finished = this.count == 1 && renderTime >= this.times[0] && position.epsilonEquals(this.target, 1.0E-4F);
      }
      if (Math.abs(this.target.x - position.x) >= 0.01F) {
         this.projectile.setDirection(position.x > this.target.x ? Direction.LEFT : Direction.RIGHT);
      }
      this.projectile.setWorldPosition(this.target);
      this.applied.set(this.target);
      if (finished) {
         this.count = 0;
         this.moving = false;
      }
   }

   private void updatePet(int id, float delta) {
      PlayerEntity owner = GameState.findPlayerById(PET_ID_BASE - id);
      if (owner == null || PetTerrain.CURRENT.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      Vector2 ownerPos = owner.getPosition();
      float ox = ownerPos.x * 100.0F; // entity units = px / 100, PetBrain uses px like the op 6 colliders
      float oy = ownerPos.y * 100.0F;
      Vector2 position = this.projectile.getPosition();
      if (this.pet == null || this.petEntityId != id) {
         this.pet = new PetBrain();
         this.petEntityId = id;
         this.pet.x = position.x * 100.0F; // op 3 already placed the pet next to its owner, on the ground
         this.pet.y = position.y * 100.0F;
         this.petOwner.lastMoveAt = 0;
         this.petResting = false;
      } else if (Math.abs(ox - this.petOwner.x) > 0.5F || Math.abs(oy - this.petOwner.y) > 0.5F) {
         this.petOwner.lastMoveAt = now;
      }
      this.petOwner.x = ox;
      this.petOwner.y = oy;
      this.petOwner.facingLeft = owner.getDirection() == Direction.LEFT;
      int result = this.pet.update(this.petOwner, PetTerrain.CURRENT, now, delta);
      if (result == PetBrain.STILL) {
         if (this.petResting) {
            return; // standing still: do not touch physics / animation
         }
         this.petResting = true; // first frame standing still: place once more so velocity = 0 -> idle animation
      } else {
         this.petResting = false;
      }
      this.projectile.setDirection(this.pet.facingLeft ? Direction.LEFT : Direction.RIGHT);
      this.target.set(this.pet.x / 100.0F, this.pet.y / 100.0F);
      if (result == PetBrain.WARPED) {
         this.snap(this.target.x, this.target.y);
      } else {
         this.projectile.setWorldPosition(this.target);
      }
   }
}
