package com.tlnt.world.anim.player.attack.whip;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class WhipSkill1Anim extends AnimationBehavior<PlayerEntity> {
   public static final WhipSkill1Anim INSTANCE = new WhipSkill1Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("whip/skill1_1")) {
         String animName = "whip/skill1_2";
         player.movementComponent.playAnimation(animName, false, 1.3F);
         player.target2.playAnimation("skill1_2", false, 1.3F);
         if (player.skeleton.currentAttackTarget != null) {
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
               animName = "hit1";
               mobEffect.tryPlayAnimation(animName, false, 1.0F);
               targetMob.floatingEffectManager.addEffect(mobEffect);
            }

            player.skeleton.currentAttackTarget = null;
            return;
         }
      } else {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "whip/skill1_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.3F);
      player.target2.equipWhip();
      player.target2.playAnimation("skill1_1", false, 1.3F);
      if (player.skeleton.currentAttackTarget != null) {
         if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.PLAYER) {
            PlayerEntity targetPlayer = (PlayerEntity)player.skeleton.currentAttackTarget;
            PooledFloatingEffect effect;
            PooledFloatingEffect effectRef = effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            effectRef.tryApplySkins(skins);
            animName = "hit1";
            effect.tryPlayAnimation(animName, false, 1.0F);
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
      }

      AudioManager.playSound("whip_skill_1", player.getCenterPosition());
   }
}
