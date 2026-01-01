package com.tlnt.world.anim.plant;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;

public final class PlantAnimDispatcher extends AnimationStateDispatcher<PlantEntity> {
   public PlantAnimDispatcher(PlantEntity entity) {
      super(entity);
   }

   @Override
   public final AnimationBehavior<PlantEntity> getAnimationBehavior(AnimationState state) {
      switch (state) {
         case PLANT_IDLE:
            return PlantIdleAnim.INSTANCE;
         default:
            return null;
      }
   }
}
