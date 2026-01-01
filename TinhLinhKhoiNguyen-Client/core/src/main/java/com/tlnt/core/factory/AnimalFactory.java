package com.tlnt.core.factory;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.animal.AnimalEntity;

public final class AnimalFactory {
   public static final ObjectPool<AnimalEntity> ANIMAL_POOL = new AnimalPool();

   public static AnimalEntity obtainAnimal(int id, float x, float y) {
      AnimalEntity animal;
      (animal = ANIMAL_POOL.obtain()).setId(id);
      animal.setAnimationState(AnimationState.ANIMAL_IDLE);
      animal.teleportTo(x, y);
      return animal;
   }

   public static void freeAnimal(AnimalEntity animal) {
      ANIMAL_POOL.free(animal);
   }
}
