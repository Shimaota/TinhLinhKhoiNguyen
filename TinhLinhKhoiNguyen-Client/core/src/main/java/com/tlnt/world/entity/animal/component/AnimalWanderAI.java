package com.tlnt.world.entity.animal.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.animal.AnimalEntity;

public final class AnimalWanderAI extends BaseComponent {
   public boolean projectile;
   public AnimalEntity speed;
   public float active = 2.0F;
   public boolean autoFlipEnabled = true;
   public float projectile2;
   public float lastTemplate = GameUtils.randomFloat(0.0F, 1.0F);
   public Vector2[] rayPoints;

   public AnimalWanderAI(AnimalEntity animal) {
      super(animal.getEntity());
      Vector2[] points = new Vector2[2];
      points[0] = new Vector2();
      points[1] = new Vector2();
      this.rayPoints = points;
      this.speed = animal;
   }

   @Override
   public final void update(float delta) {
      if (this.projectile) {
         if (this.speed.getAnimationState().allowsMovement) {
            if (this.autoFlipEnabled) {
               this.lastTemplate += delta;
               if (this.lastTemplate >= this.projectile2) {
                  this.autoFlipEnabled = false;
               }
               if (this.speed.getAnimationState() == com.tlnt.engine.physics.AnimationState.MOVE) {
                  this.speed.setAnimationState(com.tlnt.engine.physics.AnimationState.IDLE);
               }

               return;
            }

            int wallHitFlag;
            if (this.speed.getWidth() != 0.0F && this.speed.getHeight() != 0.0F) {
               Vector2 pos = this.speed.getPosition();
               boolean[] wallHitArr = new boolean[1];
               wallHitArr[0] = false;
               boolean[] wallHit = wallHitArr;
               this.rayPoints[0].set(pos.x, pos.y);
               this.rayPoints[1].set(pos.x + this.speed.getWidth() / 2.0F * this.speed.getDirection().vector.x, pos.y);
               this.speed
                  .getScreen()
                  .getPhysicsWorld()
                  .getBox2dWorld()
                  .rayCast(new AnimalWallRayCallback(this, wallHit), this.rayPoints[0], this.rayPoints[1]);
               wallHitFlag = wallHit[0] ? 1 : 0;
            } else {
               wallHitFlag = 0;
            }

            label38: {
               if (wallHitFlag == 0) {
                  Vector2 floorPos = this.speed.getPosition();
                  this.rayPoints[0].set(floorPos.x, floorPos.y + this.speed.getHeight() / 2.0F);
                  this.rayPoints[1]
                     .set(floorPos.x + (this.speed.getWidth() / 2.0F + 0.01F) * this.speed.getDirection().vector.x, floorPos.y - 0.01F);
                  boolean[] floorHitArr = new boolean[1];
                  floorHitArr[0] = false;
                  boolean[] floorHit = floorHitArr;
                  World box2dWorld = this.speed.getScreen().getPhysicsWorld().getBox2dWorld();
                  AnimalFloorRayCallback floorCallback = new AnimalFloorRayCallback(this, floorHit);
                  box2dWorld.rayCast(floorCallback, this.rayPoints[0], this.rayPoints[1]);
                  if (floorHit[0]) {
                     if (GameUtils.rollChance(1, 400)) {
                        this.reverseDirection();
                     }

                     wallHitFlag = 3;
                     if (GameUtils.rollChance(wallHitFlag, 400)) {
                        this.lastTemplate = 0.0F;
                        this.projectile2 = GameUtils.randomFloat(1.0F, 4.0F);
                        this.autoFlipEnabled = true;
                        return;
                     }
                     break label38;
                  }
               }

               this.reverseDirection();
            }

            Vector2 curPos = this.speed.getPosition();
            float dirX;
            if ((dirX = this.speed.getDirection().vector.x) == 0.0F) {
               this.speed.setDirection(Direction.RIGHT);
               dirX = this.speed.getDirection().vector.x;
            }

            float dx = this.active * delta * dirX;
            this.speed.teleportTo(curPos.x + dx, curPos.y);
            if (dx != 0.0F && this.speed.getAnimationState() == com.tlnt.engine.physics.AnimationState.IDLE) {
               this.speed.setAnimationState(com.tlnt.engine.physics.AnimationState.MOVE);
            }
         }
      }
   }

   public void reverseDirection() {
      Direction dir;
      if ((dir = this.speed.getDirection()) != Direction.LEFT && dir == Direction.RIGHT) {
         this.speed.setDirection(Direction.LEFT);
      } else {
         this.speed.setDirection(Direction.RIGHT);
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      ctx.getShapeDrawer().drawLine(this.rayPoints[0], this.rayPoints[1], 0.01F);
   }
}
