package com.tlnt.world.projectile.slingshot;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.slingshot.component.SlingshotMovementComponent;
import com.tlnt.world.projectile.slingshot.component.SlingshotSkeletonComponent;

public final class SlingshotProjectile extends GameObject {
   public static final Array<SlingshotProjectile> skeleton = new Array<>();
   public static final ObjectPool<SlingshotProjectile> POOL = new SlingshotProjectilePool();
   public PlayerEntity owner;
   public String target;
   public boolean movementComponent;
   public GameObject target2;
   public SlingshotMovementComponent movementComponent2;
   public SlingshotSkeletonComponent POOL2;

   public static SlingshotProjectile spawn(float x, float y) {
      SlingshotProjectile projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void recycle(SlingshotProjectile projectile) {
      POOL.free(projectile);
   }

   public static void recycleAll() {
      for (SlingshotProjectile projectile : skeleton) {
         POOL.free(projectile);
      }

      skeleton.clear();
   }

   public SlingshotProjectile(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
      this.movementComponent2 = new SlingshotMovementComponent(this);
      this.POOL2 = new SlingshotSkeletonComponent(this);
   }

   @Override
   public final void onStart() {
      this.setDirection(Direction.LEFT);
   }

   public final SlingshotProjectile setShooter(PlayerEntity shooter) {
      this.owner = shooter;
      return this;
   }

   public final PlayerEntity getShooter() {
      return this.owner;
   }

   public final SlingshotProjectile setDamageText(String damageText) {
      this.target = damageText;
      return this;
   }

   public final String getDamageText() {
      return this.target;
   }

   public final SlingshotProjectile setScreenShake(boolean screenShake) {
      this.movementComponent = screenShake;
      return this;
   }

   public final boolean isScreenShake() {
      return this.movementComponent;
   }

   public final SlingshotProjectile setTarget(GameObject target) {
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
