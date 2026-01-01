package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.scene.GameObject;

public final class FishingReelAnim extends AnimationBehavior<PlayerEntity> {
   public static final FishingReelAnim INSTANCE = new FishingReelAnim();

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("rod/fishing_2", true, 1.0F);
   }
}
