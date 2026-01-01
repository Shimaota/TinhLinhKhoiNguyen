package com.tlnt.world.projectile.thunderbolt;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class ThunderboltProjectilePool extends ObjectPool<ThunderboltProjectile> {

   @Override
   public ThunderboltProjectile newObject() {
      return new com.tlnt.world.projectile.thunderbolt.ThunderboltProjectile(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final ThunderboltProjectile obtain() {
      ThunderboltProjectile projectile;
      (projectile = (ThunderboltProjectile)super.obtain()).setActive(true);
      ThunderboltProjectile.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(ThunderboltProjectile obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
