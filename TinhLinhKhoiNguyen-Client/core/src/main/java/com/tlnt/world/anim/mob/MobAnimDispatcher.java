package com.tlnt.world.anim.mob;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;

public final class MobAnimDispatcher extends AnimationStateDispatcher<MobEntity> {
   public MobAnimDispatcher(MobEntity entity) {
      super(entity);
   }

   @Override
   public final AnimationBehavior<MobEntity> getAnimationBehavior(AnimationState state) {
      switch (state) {
         case MOB_IDLE:
            return MobIdleAnim.INSTANCE;
         case MOB_MOVE:
            return MobMoveAnim.INSTANCE;
         case MOB_HURT:
            return MobHitAnim.INSTANCE;
         case MOB_DIE:
            return MobDieAnim.INSTANCE;
         case MOB_ATTACK_1:
            return MobSkill1Anim.INSTANCE;
         case MOB_ATTACK_2:
            return MobSkill2Anim.INSTANCE;
         case MOB_SKILL_3:
            return MobSkill3Anim.INSTANCE;
         case MOB_SKILL_4:
            return MobSkill4Anim.INSTANCE;
         case MOB_STUN:
            return MobStunAnim.INSTANCE;
         default:
            return null;
      }
   }
}
