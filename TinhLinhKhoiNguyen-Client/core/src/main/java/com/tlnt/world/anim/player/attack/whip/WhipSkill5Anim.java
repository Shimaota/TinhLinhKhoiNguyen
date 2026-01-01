package com.tlnt.world.anim.player.attack.whip;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;

public final class WhipSkill5Anim extends AnimationBehavior<PlayerEntity> {
   public static final WhipSkill5Anim INSTANCE = new WhipSkill5Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("whip/skill5_1")) {
         player.movementComponent.playAnimation("whip/skill5_2", true, 1.0F);
         String animName = "skill5_2";
         player.target2.playAnimation(animName, true, 1.0F);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "whip/skill5_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipWhip();
      player.target2.playAnimation("skill5_1", false, 1.0F);
      AudioManager.playSound("skill5_type1", player.getCenterPosition());
   }
}
