package com.tlnt.world.anim.player.attack.whip;

import com.tlnt.world.anim.VariantAnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class WhipAttackAnim extends VariantAnimationBehavior<PlayerEntity> {
   public static final WhipAttackAnim INSTANCE = new WhipAttackAnim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player = (PlayerEntity)entity;
      if (player.skeleton.currentAttackTarget != null && player.movementComponent.getAnimationProgress(0) >= 0.3F) {
         VariantAnimationBehavior.VariantState variant = this.getOrCycleVariantState(
            player, 2, 2000, false
         );
         if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.PLAYER) {
            PlayerEntity targetPlayer = (PlayerEntity)player.skeleton.currentAttackTarget;
            PooledFloatingEffect effect;
            PooledFloatingEffect effectRef = effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            effectRef.tryApplySkins(skins);
            String hitAnim = "hit1";
            effect.tryPlayAnimation(hitAnim, false, 1.0F);
            targetPlayer.floatingEffectManager.addEffect(effect);
         } else if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.MOB) {
            MobEntity targetMob = (MobEntity)player.skeleton.currentAttackTarget;
            PooledFloatingEffect mobEffect;
            PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            String[] mobSkins = new String[1];
            mobSkins[0] = player.POOL2.weaponTexture;
            mobEffectRef.tryApplySkins(mobSkins);
            mobEffect.tryPlayAnimation("hit1", false, 1.0F);
            targetMob.floatingEffectManager.addEffect(mobEffect);
         }

         player.skeleton.currentAttackTarget = null;
      }

      if (player.movementComponent.getAnimationProgress(0) >= 1.0F) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player = (PlayerEntity)entity;
      VariantAnimationBehavior.VariantState variant = this.getOrCycleVariantState(
         player, 2, 2000, true
      );
      String animName = variant.variantIndex == 0 ? "whip/attack_1" : "whip/attack_2";
      player.movementComponent.playAnimation(animName, false, 1.1F);
      player.target2.equipWhip();
      animName = variant.variantIndex == 0 ? "attack_1" : "attack_2";
      player.target2.playAnimation(animName, false, 1.1F);
      String soundName;
      if (variant.variantIndex == 0) {
         soundName = "whip_attack_1";
      } else {
         soundName = "whip_attack_2";
      }

      AudioManager.playSound(soundName, player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
