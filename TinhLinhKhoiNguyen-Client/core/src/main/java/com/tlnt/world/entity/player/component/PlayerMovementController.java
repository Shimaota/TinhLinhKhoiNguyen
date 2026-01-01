package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Component;
import com.tlnt.engine.physics.Direction;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.decoration.WorldMapEntity;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.badlogic.gdx.physics.box2d.Fixture;

public final class PlayerMovementController extends BaseComponent {
   public boolean projectile;
   public Vector2 speed = new Vector2();
   public Vector2 active = new Vector2();
   public boolean autoFlipEnabled;
   public final Vector2 projectile2 = new Vector2(-1.0F, -1.0F);
   public PlayerMovementController.MovementCallback lastTemplate;
   public long lastDropThroughTime;
   public float jumpForce = 16.0F;
   public long lastJumpTime;
   public int jumpCount;
   public int maxJumps;
   public long lastMoveTime;
   public float lastPosX;
   public long lastServerMoveTime;
   public int serverMoveDuration;
   public GroundCheckRayCastCallback groundRayCallback;
   public PlayerEntity player;
   public Body physicsBody;
   public Vector2 lastSyncPosition;
   /**
    * Sends the position at most once per {@link #SYNC_INTERVAL_MS} (before: every frame = 60-144 packets per second). Viewers interpolate
    * between snapshots (ClickMoveController) so it stays smooth; when stopping, the final position is always sent on the next tick.
    */
   private static final long SYNC_INTERVAL_MS = 50L;
   private long lastSyncAt;

   public PlayerMovementController(PlayerEntity playerEntity) {
      super(playerEntity.getEntity());
      this.jumpCount = 0;
      this.maxJumps = 2; // double jump like the original (the decompiled code had 1)
      this.groundRayCallback = new GroundCheckRayCastCallback(this);
      this.lastSyncPosition = new Vector2();
      this.player = playerEntity;
      this.lastSyncPosition.set(this.player.getPosition());
   }

   public final void applyServerVelocity(float velocityX, float velocityY, int durationMs) {
      this.physicsBody.setLinearVelocity(velocityX, velocityY);
      this.serverMoveDuration = durationMs;
      this.lastServerMoveTime = System.currentTimeMillis();
   }

   @Override
   public final void onStart() {
      this.physicsBody = this.player.POOL.getBody();
   }

   public final void moveTowardTarget(Vector2 target, PlayerMovementController.MovementCallback callback) {
      this.setMoveTarget(target.x, target.y, callback);
   }

   public final void setMoveTarget(float x, float y, PlayerMovementController.MovementCallback callback) {
      boolean newTarget = this.projectile2.epsilonEquals(-1.0F, -1.0F) || this.projectile2.dst2(x, y) > 1.0F;
      this.projectile2.set(x, y);
      this.lastTemplate = callback;
      if (newTarget) {
         this.lastPosX = this.player.getPosition().x;
         this.lastMoveTime = System.currentTimeMillis();
         this.bestGoalDist = this.player.getPosition().dst(x, y);
         this.progressAt = this.lastMoveTime;
      }
   }

   /**
    * Auto-attack: stuck = {@link #ATTACK_STUCK_MS} without getting {@link #PROGRESS_STEP} closer to the goal (falling / jumping included, not
    * only the x axis like manual walking). Does not use the original rule "goal above + horizontally close = stuck immediately" — mobs on low
    * platforms can be jumped to, and used to be dropped immediately without jumping.
    */
   private boolean attackMoveStuck(Vector2 position) {
      float d = position.dst(this.projectile2);
      long now = System.currentTimeMillis();
      WorldEntity target = GameState.targetEntity;

      // The approach point moves with the boss. If the boss is running, the distance to the chase
      // point may not shrink even though the character is heading the right way; do not conclude the target is stuck.
      if (target != this.attackStuckTarget) {
         this.attackStuckTarget = target;
         if (target != null) {
            this.attackTargetX = target.getPosition().x;
            this.attackTargetY = target.getPosition().y;
         }
         this.bestGoalDist = d;
         this.progressAt = now;
         return false;
      }

      if (target != null) {
         Vector2 targetPosition = target.getPosition();
         if (targetPosition.dst2(this.attackTargetX, this.attackTargetY) >= TARGET_MOVEMENT_STEP * TARGET_MOVEMENT_STEP) {
            this.attackTargetX = targetPosition.x;
            this.attackTargetY = targetPosition.y;
            this.bestGoalDist = d;
            this.progressAt = now;
            return false;
         }
      }

      if (d < this.bestGoalDist - PROGRESS_STEP) {
         this.bestGoalDist = d;
         this.progressAt = now;
      }
      return now - this.progressAt > ATTACK_STUCK_MS;
   }

   private static final long ATTACK_STUCK_MS = 1500L;
   private static final float PROGRESS_STEP = 0.05F;
   private static final float TARGET_MOVEMENT_STEP = 0.08F;
   private float bestGoalDist;
   private long progressAt;
   private WorldEntity attackStuckTarget;
   private float attackTargetX;
   private float attackTargetY;

   public final void stopMovement(boolean runCallback) {
      if (runCallback && this.lastTemplate != null) {
         this.lastTemplate.execute();
      }

      this.projectile2.set(-1.0F, -1.0F);
      this.lastTemplate = null;
      this.attackStuckTarget = null;
   }

   @Override
   public final void update(float delta) {
      if (this.player.isLocalPlayer()) {
         if (this.physicsBody == null && this.player.POOL != null) {
            this.physicsBody = this.player.POOL.getBody();
         }
         if (GameUtils.hasElapsed(this.lastServerMoveTime, this.serverMoveDuration)) {
            if (this.player.isLocalPlayer() && !this.player.target.isDashing()) {
               Vector2 moveDir;
               if ((moveDir = InputHandler.getInstance().getMoveDirection()).epsilonEquals(Vector2.Zero)) {
                  InputHandler.getInstance();
                  if (InputHandler.isKeyJustPressed(Settings.keybindings[0])) {
                     this.jump();
                     this.stopMovement(false);
                  }

                  InputHandler.getInstance();
                  if (InputHandler.isKeyJustPressed(Settings.keybindings[2])) {
                     this.dropThrough();
                     this.stopMovement(false);
                  }

                  InputHandler.getInstance();
                  float keyAxis;
                  if (InputHandler.isKeyPressed(Settings.keybindings[1])) {
                     keyAxis = -1.0F;
                  } else {
                     InputHandler.getInstance();
                     keyAxis = InputHandler.isKeyPressed(Settings.keybindings[3]) ? 1.0F : 0.0F;
                  }

                  float horizontal = keyAxis;
                  if (keyAxis != 0.0F) {
                     this.player.autoTargetController.projectile = false;
                     OffscreenTargetArrow arrow = this.player.offscreenArrow;
                     this.player.stopAutoPath(); // manual movement: stop "Auto-pathing" (decompiled C3555)
                     float velocityX = Math.signum(horizontal) * this.player.POOL2.getMoveSpeedFactor();
                     this.physicsBody.setLinearVelocity(velocityX, this.physicsBody.getLinearVelocity().y);
                     this.stopMovement(false);
                     if (this.canMove()) {
                        this.player.setDirection(horizontal < 0.0F ? Direction.LEFT : Direction.RIGHT);
                     }
                  } else {
                     this.physicsBody.setLinearVelocity(0.0F, this.physicsBody.getLinearVelocity().y);
                  }
               } else {
                  int moved;
                  label228: {
                     this.player.autoTargetController.projectile = false;
                     OffscreenTargetArrow targetArrow = this.player.offscreenArrow;
                     this.player.stopAutoPath(); // manual movement: stop "Auto-pathing" (decompiled C3555)
                     float inputAngle = moveDir.angleDeg();
                     moved = 0;
                     if (inputAngle >= 60.0F && inputAngle <= 120.0F) {
                        this.jump();
                        this.stopMovement(false);
                     } else if (inputAngle >= 240.0F && inputAngle <= 300.0F) {
                        this.dropThrough();
                        this.stopMovement(false);
                     } else {
                        if ((!(inputAngle >= 300.0F) || !(inputAngle < 360.0F)) && (!(inputAngle >= 0.0F) || !(inputAngle < 60.0F)) && (!(inputAngle >= 120.0F) || !(inputAngle < 240.0F))
                           || !(Math.abs(moveDir.x) > 0.1F)) {
                           break label228;
                        }

                        float axisVelocityX = Math.signum(moveDir.x) * this.player.POOL2.getMoveSpeedFactor();
                        this.physicsBody.setLinearVelocity(axisVelocityX, this.physicsBody.getLinearVelocity().y);
                        this.stopMovement(false);
                        if (this.canMove()) {
                           this.player.setDirection(axisVelocityX < 0.0F ? Direction.LEFT : Direction.RIGHT);
                        }
                     }

                     moved = 1;
                  }

                  if (moved == 0) {
                     this.physicsBody.setLinearVelocity(0.0F, this.physicsBody.getLinearVelocity().y);
                  }
               }
            }

            if (this.player.isLocalPlayer() && !this.projectile2.epsilonEquals(-1.0F, -1.0F)) {
               Vector2 position = this.player.getPosition();
               if (this.lastTemplate != null && this.lastTemplate.isAtDestination()) {
                  this.stopMovement(true);
               } else if (this.projectile2.epsilonEquals(position, 0.5F)) {
                  this.stopMovement(true);
               } else if (this.lastTemplate instanceof MoveToTargetAndAttackCallback ? this.attackMoveStuck(position)
                  : GameUtils.hasElapsed(this.lastMoveTime, 1000)
                     // goal directly overhead: only give up when walking to an NPC / item (has a callback). Auto-pathing (no callback)
                     // must double jump onto the platform above — it used to give up every tick and so never jumped
                     || (this.lastTemplate != null && Math.abs(this.projectile2.x - position.x) <= 0.5F
                        && this.projectile2.y - position.y >= 1.0F)) {
                  if (this.lastTemplate instanceof MoveToTargetAndAttackCallback) {
                     // Stuck on terrain: temporarily drop this target to avoid spinning in place. attackMoveStuck already
                     // resets the counter when the target moves, so a running boss is not wrongly excluded.
                     this.player.autoTargetController.markUnreachable(GameState.targetEntity);
                     ((MoveToTargetAndAttackCallback)this.lastTemplate).cancel();
                     this.lastTemplate = null;
                  }
                  GameState.setTargetEntity(null);
                  this.stopMovement(true);
                  this.lastMoveTime = System.currentTimeMillis();
               } else {
                  float targetAngle = this.projectile2.x - position.x;
                  float dy;
                  targetAngle = (float)Math.toDegrees((double)((float)Math.atan2((double)(dy = this.projectile2.y - position.y), (double)targetAngle)));
                  int hasHorizontal = 0;
                  int shouldJump = 0;
                  int shouldDrop = 0;
                  Direction faceDir = null;
                  if (targetAngle > 22.5F && targetAngle < 157.5F) {
                     shouldJump = 1;
                  } else if (targetAngle < -22.5F && targetAngle > -157.5F) {
                     shouldDrop = 1;
                  }

                  if (targetAngle >= -67.5F && targetAngle <= 67.5F) {
                     hasHorizontal = 1;
                     faceDir = Direction.RIGHT;
                  } else if (targetAngle >= 112.5F || targetAngle <= -112.5F) {
                     hasHorizontal = 1;
                     faceDir = Direction.LEFT;
                  }

                  // A steep downward target used to produce only dropThrough(), even when it was
                  // horizontally offset. Keep the decompiled angle rules, but walk toward that
                  // target while descending so auto attack does not stall and flip directions.
                  if (dy < 0.0F && Math.abs(this.projectile2.x - position.x) > 0.5F && hasHorizontal == 0) {
                     hasHorizontal = 1;
                     faceDir = this.projectile2.x < position.x ? Direction.LEFT : Direction.RIGHT;
                  } else if (Math.abs(this.projectile2.x - position.x) <= 0.5F) {
                     hasHorizontal = 0;
                  }

                  if (shouldJump != 0) {
                     this.autoJump();
                  } else if (shouldDrop != 0) {
                     this.dropThrough();
                  }

                  if (hasHorizontal != 0 && this.canMove()) {
                     targetAngle = faceDir.vector.x;
                     // reuse the vector + callback (previously 2 Vector2 + 1 lambda were allocated every frame)
                     this.wallRayStart.set(position).add(targetAngle * 0.4F, 0.1F);
                     this.wallRayEnd.set(this.wallRayStart).add(targetAngle * 0.3F, 0.0F);
                     this.wallHit = false;
                     GameMain.instance.getLoginScreen().getPhysicsWorld().getBox2dWorld()
                        .rayCast(this.wallRayCallback, this.wallRayStart, this.wallRayEnd);
                     if (this.wallHit) {
                        this.autoJump();
                     } else {
                        this.physicsBody.setLinearVelocity(targetAngle * this.player.POOL2.getMoveSpeedFactor(), this.physicsBody.getLinearVelocity().y);
                     }

                     this.player.setDirection(targetAngle < 0.0F ? Direction.LEFT : Direction.RIGHT);
                  } else if (Math.abs(this.projectile2.x - this.player.getPosition().x) <= 0.5F) {
                     this.physicsBody.setLinearVelocity(0.0F, this.physicsBody.getLinearVelocity().y);
                  }

                  targetAngle = this.player.getPosition().x;
                  if (Math.abs(this.lastPosX - targetAngle) >= 0.02F) {
                     this.lastPosX = targetAngle;
                     this.lastMoveTime = System.currentTimeMillis();
                  }
               }
            }

            if (!this.canMove()) {
               this.physicsBody.setLinearVelocity(0.0F, this.physicsBody.getLinearVelocity().y);
            }

            if (!this.canMove()) {
               this.physicsBody.setLinearVelocity(0.0F, this.physicsBody.getLinearVelocity().y);
            }
         }

         Vector2 currentPos;
         long syncNow = System.currentTimeMillis();
         if (syncNow - this.lastSyncAt >= SYNC_INTERVAL_MS
               && !(currentPos = this.player.getPosition()).epsilonEquals(this.lastSyncPosition, 1.0E-5F)) {
            this.lastSyncAt = syncNow;
            this.lastSyncPosition.set(currentPos);
            this.player.getAnimationState();
            ServerPacketSender.getInstance().sendPlayerMove(currentPos.x, currentPos.y);
         }
      }
   }

   public boolean canMove() {
      if (this.player.POOL2.isAlive) {
         return false;
      } else if (!this.player.getAnimationState().allowsMovement) {
         return false;
      } else if (this.player.isDead()) {
         return false;
      } else if (this.player.harvestProgressBar != null && this.player.harvestProgressBar.projectile) {
         return false;
      } else if (this.player.rootEffect.projectile2) {
         return false;
      } else if (this.player.iceSpikeEffect.projectile2) {
         return false;
      } else {
         return true;
      }
   }

   public void jump() {
      if (this.canMove()) {
         if (this.isOnGround() || this.jumpCount < this.maxJumps) {
            this.physicsBody.setLinearVelocity(this.physicsBody.getLinearVelocity().x, 0.0F);
            this.physicsBody.applyLinearImpulse(0.0F, this.jumpForce, this.physicsBody.getWorldCenter().x, this.physicsBody.getWorldCenter().y, true);
            this.jumpCount += 1;
            this.lastJumpTime = System.currentTimeMillis();
         }
      }
   }

   /** The second jump only happens once upward velocity drops below this (near the apex of jump 1). */
   private static final float SECOND_JUMP_VY = 2.0F;
   /** Minimum wait after jump 1 (the foot raycast still touches the ground for the first few ticks). */
   private static final long SECOND_JUMP_DELAY_MS = 150L;

   /**
    * Jumping while moving AUTOMATICALLY (auto-pathing / auto-attack / walking to an NPC): on the ground -> jump 1; airborne, jumps left
    * ({@link #maxJumps} = 2) and near the apex of jump 1 -> jump 2 (a double jump like a player pressing twice). It used to call
    * {@link #jump()} every tick -> jump 2 was spent right after leaving the ground, no higher than a single jump -> at the foot of a tall
    * platform / a door above it kept jumping and never made it up.
    */
   public void autoJump() {
      if (!this.canMove()) {
         return;
      }
      if (this.isOnGround()) {
         if (System.currentTimeMillis() - this.lastJumpTime > SECOND_JUMP_DELAY_MS) {
            this.jump();
         }
         return;
      }
      if (this.jumpCount < this.maxJumps && this.physicsBody.getLinearVelocity().y <= SECOND_JUMP_VY
            && System.currentTimeMillis() - this.lastJumpTime > SECOND_JUMP_DELAY_MS) {
         this.jumpCount = Math.max(this.jumpCount, 1); // fell off an edge before jumping: one jump is still left
         this.jump();
      }
   }

   private final Vector2 wallRayStart = new Vector2();
   private final Vector2 wallRayEnd = new Vector2();
   private boolean wallHit;
   /** Only SOLID ground blocks the way (one-way TRIGGER platforms can be crossed horizontally) — the original treated every map collision as a wall. */
   private final com.badlogic.gdx.physics.box2d.RayCastCallback wallRayCallback = (fixture, point, normal, fraction) -> {
      if (fixture.getUserData() == WorldMapEntity.ColliderType.SOLID) {
         this.wallHit = true;
         return 0.0F;
      }
      return 1.0F;
   };

   public void dropThrough() {
      if (this.canMove()) {
         if (this.physicsBody == null && this.player != null && this.player.POOL != null) {
            this.physicsBody = this.player.POOL.getBody();
         }
         if (this.isOnGround()) {
            if (this.projectile) {
               this.lastDropThroughTime = System.currentTimeMillis();
               if (this.physicsBody != null) {
                  this.physicsBody.setLinearVelocity(this.physicsBody.getLinearVelocity().x, -2.0F);
               }
               return;
            }

            if (this.physicsBody != null) {
               this.physicsBody.setLinearVelocity(0.0F, 0.0F);
            }
         }
      }
   }

   public boolean isOnGround() {
      this.projectile = false;
      this.speed.set(this.getEntity().getPosition());
      this.active.set(this.getEntity().getPosition()).sub(0.0F, 0.15F);
      this.autoFlipEnabled = false;
      GameMain.instance.getLoginScreen().getPhysicsWorld().getBox2dWorld().rayCast(this.groundRayCallback, this.speed, this.active);
      if (this.autoFlipEnabled) {
         this.jumpCount = 0;
         if (this.player.getAnimationState() == AnimationState.FALL) {
            this.player.setAnimationState(AnimationState.IDLE);
         }
      }

      return this.autoFlipEnabled;
   }

   @Override
   public final boolean onContactPreSolve(Component other, Contact contact, Manifold manifold, Object userData) {
      if (userData == WorldMapEntity.ColliderType.TRIGGER) {
         if (this.physicsBody != null && this.physicsBody.getLinearVelocity().y > 0.05F) {
            contact.setEnabled(false);
            return super.onContactPreSolve(other, contact, manifold, userData);
         }

         WorldManifold worldManifold = contact.getWorldManifold();
         int numPoints = worldManifold.getNumberOfContactPoints();
         if (numPoints <= 0) {
            contact.setEnabled(false);
            return super.onContactPreSolve(other, contact, manifold, userData);
         }

         Vector2[] points = worldManifold.getPoints();
         float maxContactY = points[0].y;

         for (int i = 1; i < numPoints; i++) {
            maxContactY = Math.max(maxContactY, points[i].y);
         }

         if (!(this.player.getPosition().y < maxContactY - 0.05F)) {
            if (GameUtils.hasElapsed(this.lastDropThroughTime, 250)) {
               return super.onContactPreSolve(other, contact, manifold, userData);
            }
         }

         contact.setEnabled(false);
      }

      return super.onContactPreSolve(other, contact, manifold, userData);
   }

   @Override
   public final void onDestroy() {
      this.player = null;
      this.physicsBody = null;
      this.speed = null;
      this.active = null;
      this.lastSyncPosition = null;
      this.groundRayCallback = null;
   }

   public interface MovementCallback {
      void execute();

      boolean isAtDestination();
   }
}
