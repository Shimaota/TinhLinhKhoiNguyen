package com.tlnt.world.anim.animal;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;

public final class AnimalAnimDispatcher extends AnimationStateDispatcher<AnimalEntity> {
   public AnimalAnimDispatcher(AnimalEntity entity) {
      super(entity);
   }

   @Override
   public final AnimationBehavior<AnimalEntity> getAnimationBehavior(AnimationState state) {
      switch (state) {
         case ANIMAL_IDLE:
            return AnimalIdleAnim.INSTANCE;
         case ANIMAL_MOVE:
            return AnimalMoveAnim.INSTANCE;
         case ANIMAL_DIE:
            return AnimalDieAnim.INSTANCE;
         default:
            return null;
      }
   }
}
