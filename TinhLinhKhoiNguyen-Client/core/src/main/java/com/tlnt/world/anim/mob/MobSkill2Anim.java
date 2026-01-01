package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.player.PlayerEntity;

public final class MobSkill2Anim extends AnimationBehavior<MobEntity> {
   public static final MobSkill2Anim INSTANCE = new MobSkill2Anim();

   @Override
   public final void onUpdate(MobEntity entity, float delta) {
      MobEntity mob;
      if ((mob = (MobEntity)entity).skeleton.meleeAttackTarget != null && mob.POOL.getAnimationProgress(0) >= 0.3F) {
         if (mob.skeleton.meleeAttackTarget.getEntityType() == EntityType.PLAYER) {
            PlayerEntity targetPlayer = mob.skeleton.meleeAttackTarget;
            PooledFloatingEffect playerEffect;
            (playerEffect = PooledFloatingEffect.POOL.obtain().withDirection(mob.getDirection()))
               .tryPlayAnimation("hit9", false, 1.0F);
            targetPlayer.floatingEffectManager.addEffect(playerEffect);
         } else if (mob.skeleton.meleeAttackTarget.getEntityType() == EntityType.MOB) {
            MobEntity targetMob = (MobEntity)(Object)mob.skeleton.meleeAttackTarget;
            PooledFloatingEffect mobEffect;
            PooledFloatingEffect effectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(mob.getDirection());
            String animName = "hit9";
            effectRef.tryPlayAnimation(animName, false, 1.0F);
            targetMob.floatingEffectManager.addEffect(mobEffect);
         }

         mob.skeleton.meleeAttackTarget = null;
      }
   }

   @Override
   public final void onEnter(MobEntity entity) {
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation("skill2", false, 1.0F);
      AudioManager.playSound("mob_attack_2", mob.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(MobEntity entity, String eventName) {
      MobEntity mob;
      (mob = (MobEntity)entity).setAnimationState(AnimationState.MOB_IDLE);
   }
}
