package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class MobSkill3Anim extends AnimationBehavior<MobEntity> {
   public static final MobSkill3Anim INSTANCE = new MobSkill3Anim();

   @Override
   public final void onUpdate(MobEntity entity, float delta) {
      MobEntity mob;
      if ((mob = (MobEntity)entity).skeleton.attacking) {
         if (mob.POOL.getAnimationProgress(0) >= 0.5F) {
            GameMain.instance.getLoginScreen().box2dDebugRenderer.startShake(1.5F, 0.05F);
            mob.skeleton.attacking = false;
         }
      }
   }

   @Override
   public final void onEnter(MobEntity entity) {
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation("skill3", false, 1.0F);
      mob.skeleton.attacking = true;
      AudioManager.playSound("mob_skill_3", mob.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(MobEntity entity, String eventName) {
      MobEntity mob;
      (mob = (MobEntity)entity).setAnimationState(AnimationState.MOB_IDLE);
   }
}
