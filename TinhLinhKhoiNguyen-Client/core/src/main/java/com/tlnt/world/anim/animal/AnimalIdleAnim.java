package com.tlnt.world.anim.animal;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class AnimalIdleAnim extends AnimationBehavior<AnimalEntity> {
   public static final AnimalIdleAnim INSTANCE = new AnimalIdleAnim();

   @Override
   public final void onUpdate(AnimalEntity entity, float delta) {
      AnimalEntity animal;
      if (Math.abs((animal = (AnimalEntity)entity).getVelocity().x) >= 0.5F || Math.abs(animal.getVelocity().y) >= 0.5F) {
         animal.setAnimationState(AnimationState.ANIMAL_MOVE);
      }
   }

   @Override
   public final void onEnter(AnimalEntity entity) {
      String animName = "idle";
      AnimalEntity animal;
      (animal = (AnimalEntity)entity).skeleton.playAnimation(animName, true, 1.0F);
   }
}
