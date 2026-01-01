package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class MobHitAnim extends AnimationBehavior<MobEntity> {
   public static final MobHitAnim INSTANCE = new MobHitAnim();

   @Override
   public final void onAnimationEvent(MobEntity entity, String eventName) {
      MobEntity mob;
      (mob = (MobEntity)entity).setAnimationState(AnimationState.MOB_IDLE);
   }

   @Override
   public final void onEnter(MobEntity entity) {
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation("hit", false, 1.0F);
   }
}
