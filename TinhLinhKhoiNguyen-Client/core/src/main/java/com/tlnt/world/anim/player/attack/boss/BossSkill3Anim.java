package com.tlnt.world.anim.player.attack.boss;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class BossSkill3Anim extends AnimationBehavior<PlayerEntity> {
   public static final BossSkill3Anim INSTANCE = new BossSkill3Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill28Flag) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.4F) {
            GameMain.instance.getLoginScreen().box2dDebugRenderer.startShake(0.5F, 0.05F);
            player.skeleton.skill28Flag = false;
         }
      }

      if (player.movementComponent.getAnimationProgress(0) >= 1.0F) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "stick/skill3";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("boss_skill_3", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
