package com.tlnt.world.anim.player.attack.spear;

import com.tlnt.world.anim.VariantAnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;

public final class SpearAttackAnim extends VariantAnimationBehavior<PlayerEntity> {
   public static final SpearAttackAnim INSTANCE = new SpearAttackAnim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player = (PlayerEntity)entity;
      if (player.skeleton.currentAttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.3F) {
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
               String hitAnim = "hit1";
               mobEffect.tryPlayAnimation(hitAnim, false, 1.0F);
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
         animName = "spear/attack_1";
      } else {
         animName = "spear/attack_2";
      }

      player.movementComponent.playAnimation(animName, false, 1.3F);
      player.target2.equipSpear();
      player.target2.playAnimation(variant.variantIndex == 0 ? "attack_1" : "attack_2", false, 1.3F);
      String soundName;
      if (variant.variantIndex == 0) {
         soundName = "spear_attack_1";
      } else {
         soundName = "spear_attack_2";
      }

      AudioManager.playSound(soundName, player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
      WeaponEquipEffect weaponEffect;
      (weaponEffect = player.target2).setEnabled(false);
   }
}
