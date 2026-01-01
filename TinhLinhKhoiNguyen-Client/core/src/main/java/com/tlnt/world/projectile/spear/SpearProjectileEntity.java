package com.tlnt.world.projectile.spear;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.spear.component.SpearProjectileMovementComponent;
import com.tlnt.world.projectile.spear.component.SpearProjectileSkeleton;

public final class SpearProjectileEntity extends GameObject {
   public static final Array<SpearProjectileEntity> skeleton = new Array<>();
   public static final ObjectPool<SpearProjectileEntity> POOL = new SpearProjectilePool();
   public PlayerEntity owner;
   public GameObject target;
   public SpearProjectileMovementComponent movementComponent;
   public SpearProjectileSkeleton POOL2;

   public static SpearProjectileEntity spawn(float x, float y) {
      SpearProjectileEntity projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void release(SpearProjectileEntity projectile) {
      POOL.free(projectile);
   }

   public static void clearAll() {
      for (SpearProjectileEntity projectile : skeleton) {
         POOL.free(projectile);
      }

      skeleton.clear();
   }

   public SpearProjectileEntity(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
      this.movementComponent = new SpearProjectileMovementComponent(this);
      this.POOL2 = new SpearProjectileSkeleton(this);
   }

   @Override
   public final void onStart() {
      this.setDirection(Direction.LEFT);
   }

   public final SpearProjectileEntity setOwner(PlayerEntity owner) {
      this.owner = owner;
      return this;
   }

   public final PlayerEntity getOwner() {
      return this.owner;
   }

   public final SpearProjectileEntity setTarget(GameObject target) {
      this.target = target;
      return this;
   }

   public final GameObject getTarget() {
      return this.target;
   }

   @Override
   public final void dispose() {
   }
}
