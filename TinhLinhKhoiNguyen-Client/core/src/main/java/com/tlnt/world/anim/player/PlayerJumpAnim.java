package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class PlayerJumpAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerJumpAnim INSTANCE = new PlayerJumpAnim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      Vector2 velocity;
      if ((velocity = (player = (PlayerEntity)entity).getVelocity()).y < -0.5F) {
         player.setAnimationState(AnimationState.FALL);
      } else {
         if (velocity.y == 0.0F) {
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
      String animName = "jump";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("jump", player.getCenterPosition());
   }
}
