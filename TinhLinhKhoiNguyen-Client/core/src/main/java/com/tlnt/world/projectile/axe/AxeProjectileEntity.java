package com.tlnt.world.projectile.axe;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.axe.component.AxeProjectileHitComponent;
import com.tlnt.world.projectile.axe.component.AxeSkeletonComponent;

public final class AxeProjectileEntity extends GameObject {
   public static final Array<AxeProjectileEntity> skeleton = new Array<>();
   public static final ObjectPool<AxeProjectileEntity> POOL = new AxeProjectilePool();
   public PlayerEntity owner;
   public GameObject target;
   public AxeSkeletonComponent POOL2;
   public AxeProjectileHitComponent movementComponent;

   public static AxeProjectileEntity create(float x, float y) {
      AxeProjectileEntity projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void release(AxeProjectileEntity projectile) {
      POOL.free(projectile);
   }

   public static void releaseAll() {
      for (AxeProjectileEntity projectile : skeleton) {
         POOL.free(projectile);
      }

      skeleton.clear();
   }

   public AxeProjectileEntity(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
      this.POOL2 = new AxeSkeletonComponent(this);
      this.movementComponent = new AxeProjectileHitComponent(this);
   }

   public final AxeProjectileEntity setOwner(PlayerEntity owner) {
      this.owner = owner;
      return this;
   }

   public final PlayerEntity getOwner() {
      return this.owner;
   }

   public final AxeProjectileEntity setTarget(GameObject target) {
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
