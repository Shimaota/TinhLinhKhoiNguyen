package com.tlnt.world.anim.player;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.scene.GameObject;

public final class PlayerStunAnim extends AnimationBehavior<PlayerEntity> {
   public static final PlayerStunAnim INSTANCE = new PlayerStunAnim();

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity)
         .movementComponent
         .playAnimation("stun", true, 1.0F);
   }
}
