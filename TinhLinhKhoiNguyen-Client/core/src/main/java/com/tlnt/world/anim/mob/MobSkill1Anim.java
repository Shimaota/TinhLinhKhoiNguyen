package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.mobskill.MobSkillProjectile;

public final class MobSkill1Anim extends AnimationBehavior<MobEntity> {
   public static final MobSkill1Anim INSTANCE = new MobSkill1Anim();

   @Override
   public final void onUpdate(MobEntity entity, float delta) {
      MobEntity mob;
      if ((mob = (MobEntity)entity).POOL.getAnimationProgress(0) >= 0.5F && mob.skeleton.rangedAttackTarget != null) {
         MobSkillProjectile.create(mob.getCenterPosition().x + mob.getWidth() / 2.0F * mob.getDirection().vector.x, mob.getCenterPosition().y)
            .setTarget(mob.skeleton.rangedAttackTarget)
            .POOL2
            .playAnimation("project", true, 1.5F);
         mob.skeleton.rangedAttackTarget = null;
      }
   }

   @Override
   public final void onEnter(MobEntity entity) {
      String animName = "skill1";
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("mob_attack_1", mob.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(MobEntity entity, String eventName) {
      MobEntity mob;
      (mob = (MobEntity)entity).setAnimationState(AnimationState.MOB_IDLE);
   }
}
