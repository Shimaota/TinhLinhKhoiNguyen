package com.tlnt.world.anim.player.attack;

import com.tlnt.world.anim.VariantAnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class NormalAttackAnim extends VariantAnimationBehavior<PlayerEntity> {
   public static final NormalAttackAnim INSTANCE = new NormalAttackAnim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.currentAttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.3F) {
            if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.PLAYER) {
               PlayerEntity targetPlayer = (PlayerEntity)player.skeleton.currentAttackTarget;
               PooledFloatingEffect effect;
               (effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection()))
                  .tryPlayAnimation("hit9", false, 1.0F);
               targetPlayer.floatingEffectManager.addEffect(effect);
            } else if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.MOB) {
               MobEntity targetMob = (MobEntity)player.skeleton.currentAttackTarget;
               PooledFloatingEffect mobEffect;
               PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
               mobEffectRef.tryPlayAnimation("hit9", false, 1.0F);
               targetMob.floatingEffectManager.addEffect(mobEffect);
            }

            player.skeleton.currentAttackTarget = null;
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player = (PlayerEntity)entity;
      VariantAnimationBehavior.VariantState variant = this.getOrCycleVariantState(player, 2, 2000, true);
      String animName;
      if (variant.variantIndex == 0) {
         animName = "attack_1";
      } else {
         animName = "attack_2";
      }

      player.movementComponent.playAnimation(animName, false, 1.0F);
      AudioManager.playSound(variant.variantIndex == 0 ? "normal_attack_1" : "normal_attack_2", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
