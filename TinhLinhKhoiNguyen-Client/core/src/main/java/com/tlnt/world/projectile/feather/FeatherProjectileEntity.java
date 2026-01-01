package com.tlnt.world.projectile.feather;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.projectile.feather.component.FeatherProjectileMovement;
import com.tlnt.world.projectile.feather.component.FeatherSkeletonComponent;

public final class FeatherProjectileEntity extends GameObject {
   public static final Array<FeatherProjectileEntity> skeleton = new Array<>();
   public static final ObjectPool<FeatherProjectileEntity> POOL = new FeatherProjectilePool();
   public GameObject owner;
   public FeatherSkeletonComponent POOL2 = new FeatherSkeletonComponent(this);
   public FeatherProjectileMovement target = new FeatherProjectileMovement(this);

   public static FeatherProjectileEntity spawn(float x, float y) {
      FeatherProjectileEntity projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void release(FeatherProjectileEntity projectile) {
      POOL.free(projectile);
   }

   public FeatherProjectileEntity(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
   }

   @Override
   public final void onStart() {
      this.setDirection(Direction.LEFT);
   }

   public final FeatherProjectileEntity setTarget(WorldEntity target) {
      this.owner = target;
      return this;
   }

   public final GameObject getTarget() {
      return this.owner;
   }

   @Override
   public final void dispose() {
   }
}
