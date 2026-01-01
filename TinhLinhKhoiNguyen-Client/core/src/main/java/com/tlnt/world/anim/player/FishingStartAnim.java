package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class FishingStartAnim extends AnimationBehavior<PlayerEntity> {
   public static final FishingStartAnim INSTANCE = new FishingStartAnim();

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "rod/fishing_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("fishing_1", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.WAITING_FOR_BITE_FISHING);
   }
}
