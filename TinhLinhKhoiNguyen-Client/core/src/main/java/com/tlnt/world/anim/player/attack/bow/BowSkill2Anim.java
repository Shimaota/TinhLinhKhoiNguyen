package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class BowSkill2Anim extends AnimationBehavior<PlayerEntity> {
   public static final BowSkill2Anim INSTANCE = new BowSkill2Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      String animName = "skill2_2";
      PlayerEntity player;
      (player = (PlayerEntity)entity).secondaryWeaponEffect.playAnimation(animName, true, 1.5F);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "bow/skill2";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.25F);
      player.secondaryWeaponEffect.equipBow();
      animName = "skill2_1";
      player.secondaryWeaponEffect.playAnimation(animName, false, 1.25F);
      AudioManager.playSound("bow_skill_2", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
