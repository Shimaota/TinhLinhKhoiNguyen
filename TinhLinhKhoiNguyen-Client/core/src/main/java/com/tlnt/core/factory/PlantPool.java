package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.core.GameMain;

public final class PlantPool extends ObjectPool<PlantEntity> {
   @Override
   public PlantEntity newObject() {
      return new PlantEntity(com.tlnt.core.GameMain.instance.getLoginScreen(), 0);
   }

   @Override
   public final PlantEntity obtain() {
      PlantEntity plant;
      PlantEntity obtained = plant = (PlantEntity)super.obtain();
      obtained.setActive(true);
      return plant;
   }

   @Override
   public final void free(PlantEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
