package com.tlnt.world.projectile.thunderbolt;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.projectile.thunderbolt.component.ThunderboltProjectileComponent;
import com.tlnt.world.projectile.thunderbolt.component.ThunderboltSkeletonComponent;

public final class ThunderboltProjectile extends GameObject {
   public static final Array<ThunderboltProjectile> skeleton = new Array<>();
   public static final ObjectPool<ThunderboltProjectile> POOL = new ThunderboltProjectilePool();
   public GameObject owner;
   public ThunderboltSkeletonComponent POOL2 = new ThunderboltSkeletonComponent(this);
   public ThunderboltProjectileComponent target = new ThunderboltProjectileComponent(this);

   public static ThunderboltProjectile obtain(float x, float y) {
      ThunderboltProjectile projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void free(ThunderboltProjectile projectile) {
      POOL.free(projectile);
   }

   public ThunderboltProjectile(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
   }

   @Override
   public final void onStart() {
      this.setDirection(Direction.LEFT);
   }

   public final ThunderboltProjectile setTarget(GameObject target) {
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
