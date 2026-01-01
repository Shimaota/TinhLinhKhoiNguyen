package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.core.GameMain;
import com.tlnt.screen.login.LoginScreen;

public final class MobPool extends ObjectPool<MobEntity> {
   @Override
   public MobEntity newObject() {
      return new MobEntity(com.tlnt.core.GameMain.instance.getLoginScreen(), 0, 0.0F, 0.0F);
   }

   @Override
   public final MobEntity obtain() {
      MobEntity mob;
      MobEntity obtained = mob = (MobEntity)super.obtain();
      obtained.setActive(true);
      return mob;
   }

   @Override
   public final void free(MobEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
