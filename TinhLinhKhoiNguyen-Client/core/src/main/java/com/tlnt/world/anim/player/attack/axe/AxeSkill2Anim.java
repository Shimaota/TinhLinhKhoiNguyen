package com.tlnt.world.anim.player.attack.axe;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;

public final class AxeSkill2Anim extends AnimationBehavior<PlayerEntity> {
   public static final AxeSkill2Anim INSTANCE = new AxeSkill2Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("axe/skill2_1")) {
         String animName = "axe/skill2_2";
         player.movementComponent.playAnimation(animName, true, 1.0F);
         animName = "skill2_2";
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
      String animName = "axe/skill2_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipAxe();
      player.target2.playAnimation("skill2_1", false, 1.0F);
      AudioManager.playSound("axe_skill_2", player.getCenterPosition());
   }
}
