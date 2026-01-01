package com.tlnt.world.anim.player.attack.spear;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;

public final class SpearSkill5Anim extends AnimationBehavior<PlayerEntity> {
   public static final SpearSkill5Anim INSTANCE = new SpearSkill5Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("spear/skill5_1")) {
         player.movementComponent.playAnimation("spear/skill5_2", true, 1.1F);
         String animName = "skill5_2";
         player.target2.playAnimation(animName, true, 1.1F);
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
      (player = (PlayerEntity)entity).movementComponent.playAnimation("spear/skill5_1", false, 1.1F);
      player.target2.equipSpear();
      String animName = "skill5_1";
      player.target2.playAnimation(animName, false, 1.1F);
      AudioManager.playSound("skill5_type2", player.getCenterPosition());
   }
}
