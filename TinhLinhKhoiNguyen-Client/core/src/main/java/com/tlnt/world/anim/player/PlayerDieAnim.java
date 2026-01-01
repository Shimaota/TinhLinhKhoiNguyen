package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.scene.GameObject;

public final class PlayerDieAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerDieAnim INSTANCE = new PlayerDieAnim();

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "die";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
   }
}
