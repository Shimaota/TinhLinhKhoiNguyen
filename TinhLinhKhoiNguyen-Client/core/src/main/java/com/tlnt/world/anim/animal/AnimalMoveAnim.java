package com.tlnt.world.anim.animal;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class AnimalMoveAnim extends AnimationBehavior<AnimalEntity> {
   public static final AnimalMoveAnim INSTANCE = new AnimalMoveAnim();

   @Override
   public final void onUpdate(AnimalEntity entity, float delta) {
      AnimalEntity animal;
      if (Math.abs((animal = (AnimalEntity)entity).getVelocity().x) < 0.5F && Math.abs(animal.getVelocity().y) < 0.5F) {
         animal.setAnimationState(AnimationState.ANIMAL_IDLE);
      }
   }

   @Override
   public final void onEnter(AnimalEntity entity) {
      AnimalEntity animal;
      (animal = (AnimalEntity)entity).skeleton.playAnimation("move", true, 1.0F);
   }
}
