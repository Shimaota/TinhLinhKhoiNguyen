package com.tlnt.world.projectile.bow;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class BowArrowProjectilePool extends ObjectPool<BowArrowProjectile> {

   @Override
   public BowArrowProjectile newObject() {
      return new com.tlnt.world.projectile.bow.BowArrowProjectile(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final BowArrowProjectile obtain() {
      BowArrowProjectile projectile;
      (projectile = (BowArrowProjectile)super.obtain()).setActive(true);
      BowArrowProjectile.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(BowArrowProjectile obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
