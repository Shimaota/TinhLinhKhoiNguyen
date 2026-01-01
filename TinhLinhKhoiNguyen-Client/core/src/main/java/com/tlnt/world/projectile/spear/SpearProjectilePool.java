package com.tlnt.world.projectile.spear;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class SpearProjectilePool extends ObjectPool<SpearProjectileEntity> {

   @Override
   public SpearProjectileEntity newObject() {
      return new com.tlnt.world.projectile.spear.SpearProjectileEntity(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final SpearProjectileEntity obtain() {
      SpearProjectileEntity projectile;
      (projectile = (SpearProjectileEntity)super.obtain()).setActive(true);
      SpearProjectileEntity.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(SpearProjectileEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
