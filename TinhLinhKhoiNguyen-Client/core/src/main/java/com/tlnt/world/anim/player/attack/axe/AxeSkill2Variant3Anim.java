package com.tlnt.world.anim.player.attack.axe;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class AxeSkill2Variant3Anim extends AnimationBehavior<PlayerEntity> {
   public static final AxeSkill2Variant3Anim INSTANCE = new AxeSkill2Variant3Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("axe/skill2_3")) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "axe/skill2_3";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipAxe();
      animName = "skill2_3";
      player.target2.playAnimation(animName, false, 1.0F);
   }
}
