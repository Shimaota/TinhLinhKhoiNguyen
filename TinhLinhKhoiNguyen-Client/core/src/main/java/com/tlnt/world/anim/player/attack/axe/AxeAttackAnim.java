package com.tlnt.world.anim.player.attack.axe;

import com.tlnt.world.anim.VariantAnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class AxeAttackAnim extends VariantAnimationBehavior<PlayerEntity> {
   public static final AxeAttackAnim INSTANCE = new AxeAttackAnim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("axe/attack_1") || eventName.equals("axe/attack_2")) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

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
            effect.tryPlayAnimation("hit1", false, 1.0F);
            targetPlayer.floatingEffectManager.addEffect(effect);
         } else if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.MOB) {
            MobEntity targetMob = (MobEntity)player.skeleton.currentAttackTarget;
            PooledFloatingEffect mobEffect;
            PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            String[] mobSkins = new String[1];
            mobSkins[0] = player.POOL2.weaponTexture;
            mobEffectRef.tryApplySkins(mobSkins);
            String mobAnimName = "hit1";
            mobEffect.tryPlayAnimation(mobAnimName, false, 1.0F);
            targetMob.floatingEffectManager.addEffect(mobEffect);
         }

         player.skeleton.currentAttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player = (PlayerEntity)entity;
      VariantAnimationBehavior.VariantState variant = this.getOrCycleVariantState(player, 2, 2000, true);
      String animName;
      if (variant.variantIndex == 0) {
         animName = "axe/attack_1";
      } else {
         animName = "axe/attack_2";
      }

      player.movementComponent.playAnimation(animName, false, 1.1F);
      player.target2.equipAxe();
      player.target2
         .playAnimation(
            variant.variantIndex == 0 ? "attack_1" : "attack_2",
            false,
            1.1F
         );
      AudioManager.playSound(variant.variantIndex == 0 ? "axe_attack_1" : "axe_attack_2", player.getCenterPosition());
   }
}
