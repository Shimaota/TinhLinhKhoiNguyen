package com.tlnt.world.projectile.axe;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class AxeProjectilePool extends ObjectPool<AxeProjectileEntity> {

   @Override
   public AxeProjectileEntity newObject() {
      return new com.tlnt.world.projectile.axe.AxeProjectileEntity(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final AxeProjectileEntity obtain() {
      AxeProjectileEntity projectile;
      AxeProjectileEntity obtained = projectile = (AxeProjectileEntity)super.obtain();
      obtained.setActive(true);
      AxeProjectileEntity.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(AxeProjectileEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
