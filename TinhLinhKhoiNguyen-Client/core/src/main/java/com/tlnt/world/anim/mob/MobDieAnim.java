package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class MobDieAnim extends AnimationBehavior<MobEntity> {
   public static final MobDieAnim INSTANCE = new MobDieAnim();

   @Override
   public final void onAnimationEvent(MobEntity entity, String eventName) {
      MobEntity mob;
      (mob = (MobEntity)entity).setAnimationState(AnimationState.MOB_IDLE);
      mob.setActive(false);
   }

   @Override
   public final void onEnter(MobEntity entity) {
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation("die", false, 1.0F);
   }
}
