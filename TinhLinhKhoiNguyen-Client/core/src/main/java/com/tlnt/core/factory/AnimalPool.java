package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.core.GameMain;

public final class AnimalPool extends ObjectPool<AnimalEntity> {
   @Override
   public AnimalEntity newObject() {
      return new AnimalEntity(com.tlnt.core.GameMain.instance.getLoginScreen(), 0);
   }

   @Override
   public final AnimalEntity obtain() {
      AnimalEntity animal;
      (animal = (AnimalEntity)super.obtain()).setActive(true);
      return animal;
   }

   @Override
   public final void free(AnimalEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
