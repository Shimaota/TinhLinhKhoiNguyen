package com.tlnt.world.anim.player.attack.boss;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class BossSkill5Anim extends AnimationBehavior<PlayerEntity> {
   public static final BossSkill5Anim INSTANCE = new BossSkill5Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).movementComponent.getAnimationProgress(0) >= 1.0F) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity)
         .movementComponent
         .playAnimation("stick/skill5", false, 1.0F);
      AudioManager.playSound("boss_skill_special", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
