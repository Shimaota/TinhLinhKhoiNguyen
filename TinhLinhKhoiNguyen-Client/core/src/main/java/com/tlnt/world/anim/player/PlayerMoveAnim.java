package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class PlayerMoveAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerMoveAnim INSTANCE = new PlayerMoveAnim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      AudioManager.stopLoopedSound(player = (PlayerEntity)entity, "move");
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if (!(player = (PlayerEntity)entity).isLocalPlayer() && PlayerEntity.getInstance() != null && PlayerEntity.getInstance().distanceTo(player) > 10.0F) {
         AudioManager.stopLoopedSound(player, "move");
      } else if (AudioManager.getLoopedSoundIdForPlayer(player, "move") == -1L) {
         AudioManager.loopSound(player, "move", player.getCenterPosition());
      }

      Vector2 velocity;
      if ((velocity = player.getVelocity()).y > 0.5F) {
         player.setAnimationState(AnimationState.JUMP);
      } else if (velocity.y < -0.5F) {
         player.setAnimationState(AnimationState.FALL);
      } else {
         if (Math.abs(velocity.x) < 0.5F) {
            player.setAnimationState(AnimationState.IDLE);
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("run", true, 1.0F);
      AudioManager.loopSound(player, "move", player.getCenterPosition());
   }
}
