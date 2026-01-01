package com.tlnt.world.entity.mob.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.mob.MobEntity;

public final class MobWanderAI extends BaseComponent {
   public MobEntity speed;
   public Vector2 projectile = new Vector2(-1.0F, -1.0F);
   public boolean active;
   public float autoFlipEnabled;
   public float projectile2;
   public float lastTemplate = 4.0F;
   public Vector2[] rayCastPoints;

   public MobWanderAI(MobEntity mob) {
      super(mob.getEntity());
      Vector2[] points = new Vector2[2];
      points[0] = new Vector2();
      points[1] = new Vector2();
      this.rayCastPoints = points;
      this.speed = mob;
   }

   @Override
   public final void update(float delta) {
      if (!this.speed.isDead()) {
         int stateFlag;
         if (!this.speed.getAnimationState().allowsMovement) {
            stateFlag = 0;
         } else if (this.speed.stunEffect.projectile2) {
            stateFlag = 0;
         } else {
            stateFlag = 1;
         }

         if (stateFlag != 0) {
            if (this.active) {
               this.projectile2 += delta;
               if (this.projectile2 >= this.autoFlipEnabled) {
                  this.active = false;
               }
               if (this.speed.getAnimationState() == com.tlnt.engine.physics.AnimationState.MOB_MOVE) {
                  this.speed.setAnimationState(com.tlnt.engine.physics.AnimationState.MOB_IDLE);
               }

               return;
            }

            label81: {
               if (this.speed.POOL2 != null && this.speed.POOL2.template != null) {
                  if (this.speed.POOL2.template.wanderType == 2) {
                     stateFlag = 1;
                     break label81;
                  }
               }

               stateFlag = 0;
            }

            int randomWander;
            randomWander = stateFlag;
            label75:
            if (stateFlag == 0) {
               if (!this.isBlockedByTerrain()) {
                  Vector2 pos = this.speed.getPosition();
                  this.rayCastPoints[0].set(pos.x, pos.y + this.speed.getHeight() / 2.0F);
                  this.rayCastPoints[1]
                     .set(pos.x + (this.speed.getWidth() / 2.0F + 0.01F) * this.speed.getDirection().vector.x, pos.y - 0.01F);
                  boolean[] edgeHitArr = new boolean[1];
                  edgeHitArr[0] = false;
                  boolean[] edgeHit = edgeHitArr;
                  World box2dWorld = this.speed.getScreen().getPhysicsWorld().getBox2dWorld();
                  MobEdgeRayCallback edgeCallback = new MobEdgeRayCallback(this, edgeHit);
                  box2dWorld.rayCast(edgeCallback, this.rayCastPoints[0], this.rayCastPoints[1]);
                  if (edgeHit[0]) {
                     break label75;
                  }
               }

               this.projectile.set(-1.0F, -1.0F);
            } else if (this.isBlockedByTerrain()) {
               this.projectile.set(-1.0F, -1.0F);
            }

            if (this.projectile.epsilonEquals(-1.0F, -1.0F)) {
               float coord = this.speed.getPosition().x;
               float spawnX = this.speed.POOL2.spawnPosition.x;
               float wanderRange = (float)this.speed.POOL2.template.wanderRange / 100.0F;
               if (randomWander == 0) {
                  if (coord < spawnX) {
                     this.projectile.set(spawnX + wanderRange, this.speed.POOL2.spawnPosition.y);
                  } else {
                     this.projectile.set(spawnX - wanderRange, this.speed.POOL2.spawnPosition.y);
                  }
               } else {
                  float centerX = this.speed.POOL2.spawnPosition.x;
                  coord = this.speed.POOL2.spawnPosition.y;
                  float rangeY = wanderRange * 0.5F;
                  float radiusFactor = (float)Math.pow((double)GameUtils.randomFloat01(), (double)(1.0F / this.lastTemplate));
                  float angleRad = (float)((double)GameUtils.randomFloat01() * Math.PI * 2.0);
                  float targetX = centerX + wanderRange * radiusFactor * (float)Math.cos((double)angleRad);
                  coord += rangeY * radiusFactor * (float)Math.sin((double)angleRad);
                  this.projectile.set(targetX, coord);
               }
            }

            Vector2 curPos = this.speed.getPosition();
            Vector2 moveVec;
            float distance = (moveVec = this.projectile.cpy().sub(curPos)).len();
            if (moveVec.nor().scl(this.speed.POOL2.template.variants[this.speed.POOL2.variantIndex].moveSpeed).scl(delta).len() > distance) {
               this.projectile.set(-1.0F, -1.0F);
               stateFlag = 5;
               if (GameUtils.rollChance(5, 100)) {
                  this.startWaitPause();
               }

               return;
            }

            if (this.speed.target.projectile2 || this.speed.movementComponent.projectile2) {
               moveVec.scl(0.0F);
            }

            this.speed.teleportTo(curPos.x + moveVec.x, curPos.y + moveVec.y);
            if (moveVec.x != 0.0F && this.speed.getAnimationState().allowsMovement) {
               this.speed.setDirection(moveVec.x < 0.0F ? Direction.LEFT : Direction.RIGHT);
               if (this.speed.getAnimationState() == com.tlnt.engine.physics.AnimationState.MOB_IDLE) {
                  this.speed.setAnimationState(com.tlnt.engine.physics.AnimationState.MOB_MOVE);
               }
            }

            return;
         }
      }
   }

   public final void startWaitPause() {
      this.projectile2 = 0.0F;
      this.autoFlipEnabled = GameUtils.randomFloat(1.0F, 5.0F);
      this.active = true;
      if (this.speed.getAnimationState() == com.tlnt.engine.physics.AnimationState.MOB_MOVE) {
         this.speed.setAnimationState(com.tlnt.engine.physics.AnimationState.MOB_IDLE);
      }
   }

   public boolean isBlockedByTerrain() {
      if (this.speed.getWidth() != 0.0F && this.speed.getHeight() != 0.0F) {
         Vector2 pos = this.speed.getPosition();
         boolean[] hitArr = new boolean[1];
         hitArr[0] = false;
         boolean[] hit = hitArr;
         this.rayCastPoints[0].set(pos.x, pos.y);
         this.rayCastPoints[1].set(pos.x + this.speed.getWidth() / 2.0F * this.speed.getDirection().vector.x, pos.y);
         this.speed
            .getScreen()
            .getPhysicsWorld()
            .getBox2dWorld()
            .rayCast(
               new MobTerrainRayCallback(this, hit),
               this.rayCastPoints[0],
               this.rayCastPoints[1]
            );
         return hit[0];
      } else {
         return false;
      }
   }

   @Override
   public final void render(RenderContext ctx) {
   }
}
