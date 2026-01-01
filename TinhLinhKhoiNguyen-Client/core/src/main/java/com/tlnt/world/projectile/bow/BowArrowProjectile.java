package com.tlnt.world.projectile.bow;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.bow.component.BowArrowMovementComponent;
import com.tlnt.world.projectile.bow.component.BowArrowSkeletonComponent;

public final class BowArrowProjectile extends GameObject {
   public static final Array<BowArrowProjectile> skeleton = new Array<>();
   public static final ObjectPool<BowArrowProjectile> POOL = new BowArrowProjectilePool();
   public PlayerEntity owner;
   public String target;
   public boolean movementComponent;
   public GameObject target2;
   public BowArrowSkeletonComponent POOL2 = new BowArrowSkeletonComponent(this);
   public BowArrowMovementComponent movementComponent2 = new BowArrowMovementComponent(this);

   public static BowArrowProjectile spawn(float x, float y) {
      BowArrowProjectile projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void release(BowArrowProjectile projectile) {
      POOL.free(projectile);
   }

   public static void clearAll() {
      for (BowArrowProjectile projectile : skeleton) {
         POOL.free(projectile);
      }

      skeleton.clear();
   }

   public BowArrowProjectile(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
   }

   @Override
   public final void onStart() {
      this.setDirection(Direction.LEFT);
   }

   public final BowArrowProjectile setHitSoundName(String hitSoundName) {
      this.target = hitSoundName;
      return this;
   }

   public final String getHitSoundName() {
      return this.target;
   }

   public final BowArrowProjectile setOwner(PlayerEntity owner) {
      this.owner = owner;
      return this;
   }

   public final PlayerEntity getOwner() {
      return this.owner;
   }

   public final BowArrowProjectile setShakesCamera(boolean shakesCamera) {
      this.movementComponent = shakesCamera;
      return this;
   }

   public final boolean isShakesCamera() {
      return this.movementComponent;
   }

   public final BowArrowProjectile setTarget(GameObject target) {
      this.target2 = target;
      return this;
   }

   public final GameObject getTarget() {
      return this.target2;
   }

   @Override
   public final void dispose() {
   }
}
