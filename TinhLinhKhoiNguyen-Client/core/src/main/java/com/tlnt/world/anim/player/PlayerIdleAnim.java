package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class PlayerIdleAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerIdleAnim INSTANCE = new PlayerIdleAnim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      Vector2 velocity = (player = (PlayerEntity)entity).getVelocity();
      if (player.isDead()) {
         player.setAnimationState(AnimationState.DIE);
      } else if (velocity.y > 0.5F) {
         player.setAnimationState(AnimationState.JUMP);
      } else if (velocity.y < -0.5F) {
         player.setAnimationState(AnimationState.FALL);
      } else {
         if (Math.abs(velocity.x) >= 0.5F) {
            player.setAnimationState(AnimationState.MOVE);
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "idle";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, true, 1.0F);
   }
}
