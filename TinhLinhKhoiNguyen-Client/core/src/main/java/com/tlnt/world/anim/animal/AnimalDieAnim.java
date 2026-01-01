package com.tlnt.world.anim.animal;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.core.factory.AnimalFactory;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class AnimalDieAnim extends AnimationBehavior<AnimalEntity> {
   public static final AnimalDieAnim INSTANCE = new AnimalDieAnim();

   @Override
   public final void onAnimationEvent(AnimalEntity entity, String eventName) {
      AnimalEntity animal;
      (animal = (AnimalEntity)entity).setAnimationState(AnimationState.ANIMAL_IDLE);
      AnimalFactory.freeAnimal(animal);
   }

   @Override
   public final void onEnter(AnimalEntity entity) {
      AnimalEntity animal;
      (animal = (AnimalEntity)entity).skeleton.playAnimation("die", false, 1.0F);
   }
}
