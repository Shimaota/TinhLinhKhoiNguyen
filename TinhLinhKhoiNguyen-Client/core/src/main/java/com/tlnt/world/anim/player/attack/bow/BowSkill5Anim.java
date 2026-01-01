package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;

public final class BowSkill5Anim extends AnimationBehavior<PlayerEntity> {
   public static final BowSkill5Anim INSTANCE = new BowSkill5Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("bow/skill5_1")) {
         player.movementComponent.playAnimation("bow/skill5_2", true, 1.15F);
         player.target2.playAnimation("skill5_2", true, 1.15F);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "bow/skill5_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipBow();
      animName = "skill5_1";
      player.target2.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("skill5_type2", player.getCenterPosition());
   }
}
