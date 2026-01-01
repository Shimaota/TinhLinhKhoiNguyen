package com.tlnt.world.anim.player.attack.whip;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.GameMain;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;

public final class WhipSkill5Variant3Anim extends AnimationBehavior<PlayerEntity> {
   public static final WhipSkill5Variant3Anim INSTANCE = new WhipSkill5Variant3Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill29Flag && player.movementComponent.getCurrentAnimationName().equals("whip/skill5_3")
         )
       {
         if (player.movementComponent.getAnimationProgress(0) >= 0.2F) {
            player.skeleton.skill29Flag = false;
            GameMain.instance.getLoginScreen().box2dDebugRenderer.startShake(0.5F, 0.05F);
            if (player.skeleton.currentAttackTarget != null) {
               if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.PLAYER) {
                  PlayerEntity targetPlayer = (PlayerEntity)player.skeleton.currentAttackTarget;
                  PooledFloatingEffect effect;
                  PooledFloatingEffect effectRef = effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
                  String[] skins = new String[1];
                  skins[0] = player.POOL2.weaponTexture;
                  effectRef.tryApplySkins(skins);
                  effect.tryPlayAnimation("hit5", false, 1.0F);
                  targetPlayer.floatingEffectManager.addEffect(effect);
               } else if (player.skeleton.currentAttackTarget.getEntityType() == EntityType.MOB) {
                  MobEntity targetMob = (MobEntity)player.skeleton.currentAttackTarget;
                  PooledFloatingEffect mobEffect;
                  PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
                  String[] mobSkins = new String[1];
                  mobSkins[0] = player.POOL2.weaponTexture;
                  mobEffectRef.tryApplySkins(mobSkins);
                  mobEffect.tryPlayAnimation("hit5", false, 1.0F);
                  targetMob.floatingEffectManager.addEffect(mobEffect);
               }

               player.skeleton.currentAttackTarget = null;
            }
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "whip/skill5_3";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipWhip();
      animName = "skill5_3";
      player.target2.playAnimation(animName, false, 1.0F);
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
