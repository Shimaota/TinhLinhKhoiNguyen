package com.tlnt.world.anim.player.attack.spear;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class SpearSkill2Anim extends AnimationBehavior<PlayerEntity> {
   public static final SpearSkill2Anim INSTANCE = new SpearSkill2Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.currentAttackTarget != null
         && player.movementComponent.getAnimationProgress(0) >= 0.3F) {
         if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.PLAYER) {
            PlayerEntity targetPlayer = (PlayerEntity)player.skeleton.currentAttackTarget;
            PooledFloatingEffect effect;
            PooledFloatingEffect effectRef = effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            effectRef.tryApplySkins(skins);
            String hitAnim = "hit2";
            effect.tryPlayAnimation(hitAnim, false, 1.0F);
            targetPlayer.floatingEffectManager.addEffect(effect);
         } else if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.MOB) {
            MobEntity targetMob = (MobEntity)player.skeleton.currentAttackTarget;
            PooledFloatingEffect mobEffect;
            PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            String[] mobSkins = new String[1];
            mobSkins[0] = player.POOL2.weaponTexture;
            mobEffectRef.tryApplySkins(mobSkins);
            mobEffect.tryPlayAnimation("hit2", false, 1.0F);
            targetMob.floatingEffectManager.addEffect(mobEffect);
         }

         player.skeleton.currentAttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("spear/skill2", false, 1.5F);
      player.target2.equipSpear();
      String animName = "skill2";
      player.target2.playAnimation(animName, false, 1.5F);
      AudioManager.playSound("spear_skill_2", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
