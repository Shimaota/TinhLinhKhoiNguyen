package com.tlnt.core.factory;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.plant.PlantEntity;

public final class PlantFactory {
   public static final ObjectPool<PlantEntity> PLANT_POOL = new PlantPool();

   public static PlantEntity obtainPlant(int id, float x, float y) {
      PlantEntity plant;
      (plant = PLANT_POOL.obtain()).setId(id);
      plant.setAnimationState(AnimationState.PLANT_IDLE);
      plant.teleportTo(x, y);
      return plant;
   }

   public static void freePlant(PlantEntity plant) {
      PLANT_POOL.free(plant);
   }
}
