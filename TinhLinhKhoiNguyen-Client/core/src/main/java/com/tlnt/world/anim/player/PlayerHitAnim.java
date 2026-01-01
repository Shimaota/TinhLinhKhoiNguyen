package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class PlayerHitAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerHitAnim INSTANCE = new PlayerHitAnim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).movementComponent.getAnimationProgress(0) >= 1.0F) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "hit";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
   }
}
