package com.tlnt.world.anim.player.attack.spear;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class SpearSkill1Anim extends AnimationBehavior<PlayerEntity> {
   public static final SpearSkill1Anim INSTANCE = new SpearSkill1Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      int eventIndex = -1;
      switch (eventName.hashCode()) {
         case -995512024:
            if (eventName.equals("spear/skill1_1")) {
               eventIndex = 0;
            }
            break;
         case -995512023:
            if (eventName.equals("spear/skill1_2")) {
               eventIndex = 1;
            }
            break;
         case -995512022:
            if (eventName.equals("spear/skill1_3")) {
               eventIndex = 2;
            }
      }

      switch (eventIndex) {
         case 0:
            player.movementComponent.playAnimation("spear/skill1_2", false, 1.0F);
            String animName = "skill1_2";
            player.target2.playAnimation(animName, false, 1.0F);
            return;
         case 1:
            String nextAnimName = "spear/skill1_3";
            player.movementComponent.playAnimation(nextAnimName, false, 1.5F);
            nextAnimName = "skill1_3";
            player.target2.playAnimation(nextAnimName, false, 1.5F);
            return;
         case 2:
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
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.currentAttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.3F) {
            if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.PLAYER) {
               PlayerEntity targetPlayer = (PlayerEntity)player.skeleton.currentAttackTarget;
               PooledFloatingEffect effect;
               PooledFloatingEffect effectRef = effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
               String[] skins = new String[1];
               skins[0] = player.POOL2.weaponTexture;
               effectRef.tryApplySkins(skins);
               String hitAnim = "hit4";
               effect.tryPlayAnimation(hitAnim, false, 1.0F);
               targetPlayer.floatingEffectManager.addEffect(effect);
            } else if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.MOB) {
               MobEntity targetMob = (MobEntity)player.skeleton.currentAttackTarget;
               PooledFloatingEffect mobEffect;
               PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
               String[] mobSkins = new String[1];
               mobSkins[0] = player.POOL2.weaponTexture;
               mobEffectRef.tryApplySkins(mobSkins);
               String mobHitAnim = "hit4";
               mobEffect.tryPlayAnimation(mobHitAnim, false, 1.0F);
               targetMob.floatingEffectManager.addEffect(mobEffect);
            }

            player.skeleton.currentAttackTarget = null;
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity)
         .movementComponent
         .playAnimation("spear/skill1_1", false, 1.0F);
      player.target2.equipSpear();
      String animName = "skill1_1";
      player.target2.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("spear_skill_1", player.getCenterPosition());
   }
}
