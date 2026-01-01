package com.tlnt.world.anim.player.attack.axe;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;

public final class AxeSkill5Anim extends AnimationBehavior<PlayerEntity> {
   public static final AxeSkill5Anim INSTANCE = new AxeSkill5Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("axe/skill5_1")) {
         player.movementComponent.playAnimation("axe/skill5_2", true, 1.0F);
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
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("axe/skill5_1", false, 1.5F);
      player.target2.equipAxe();
      player.target2.playAnimation("skill5_1", false, 1.5F);
      AudioManager.playSound("skill5_type1", player.getCenterPosition());
   }
}
