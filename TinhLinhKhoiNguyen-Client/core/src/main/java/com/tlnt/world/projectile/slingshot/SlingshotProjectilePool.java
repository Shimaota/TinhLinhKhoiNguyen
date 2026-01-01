package com.tlnt.world.projectile.slingshot;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class SlingshotProjectilePool extends ObjectPool<SlingshotProjectile> {

   @Override
   public SlingshotProjectile newObject() {
      return new com.tlnt.world.projectile.slingshot.SlingshotProjectile(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final SlingshotProjectile obtain() {
      SlingshotProjectile projectile;
      (projectile = (SlingshotProjectile)super.obtain()).setActive(true);
      SlingshotProjectile.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(SlingshotProjectile obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
