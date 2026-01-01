package com.tlnt.world.anim.player;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;

public final class PlayerFallAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerFallAnim INSTANCE = new PlayerFallAnim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player = (PlayerEntity)entity;
      switch (player.getAnimationState()) {
         case MOVE:
         case IDLE:
            AudioManager.playSound("hit_ground", player.getCenterPosition());
      }
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player = (PlayerEntity)entity;
      if (player.movementController != null && player.movementController.isOnGround()) {
         if (Math.abs(player.getVelocity().x) >= 0.5F) {
            player.setAnimationState(AnimationState.MOVE);
         } else {
            player.setAnimationState(AnimationState.IDLE);
         }
         return;
      }
      Vector2 velocity;
      if ((velocity = player.getVelocity()).y > 0.5F) {
         player.setAnimationState(AnimationState.JUMP);
      } else {
         if (Math.abs(velocity.y) <= 0.5F) {
            if (Math.abs(velocity.x) >= 0.5F) {
               player.setAnimationState(AnimationState.MOVE);
               return;
            }

            player.setAnimationState(AnimationState.IDLE);
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("fall", false, 1.0F);
   }
}
