package com.tlnt.world.projectile.feather;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class FeatherProjectilePool extends ObjectPool<FeatherProjectileEntity> {

   @Override
   public FeatherProjectileEntity newObject() {
      return new com.tlnt.world.projectile.feather.FeatherProjectileEntity(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final FeatherProjectileEntity obtain() {
      FeatherProjectileEntity projectile;
      (projectile = (FeatherProjectileEntity)super.obtain()).setActive(true);
      FeatherProjectileEntity.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(FeatherProjectileEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
