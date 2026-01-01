package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class MobIdleAnim extends AnimationBehavior<MobEntity> {
   public static final MobIdleAnim INSTANCE = new MobIdleAnim();

   @Override
   public final void onUpdate(MobEntity entity, float delta) {
      MobEntity mob;
      if (Math.abs((mob = (MobEntity)entity).getVelocity().x) >= 0.5F || Math.abs(mob.getVelocity().y) >= 0.5F) {
         mob.setAnimationState(AnimationState.MOB_MOVE);
      }
   }

   @Override
   public final void onEnter(MobEntity entity) {
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation("idle", true, 1.0F);
   }
}
