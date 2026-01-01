package com.tlnt.world.anim.player.attack.whip;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;

public final class WhipSkill2Anim extends AnimationBehavior<PlayerEntity> {
   public static final WhipSkill2Anim INSTANCE = new WhipSkill2Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      if (!(player = (PlayerEntity)entity).target.isDashing()) {
         player.setAnimationState(AnimationState.IDLE);
         WeaponEquipEffect weaponEffect;
         WeaponEquipEffect weaponEffectRef = weaponEffect = player.target2;
         weaponEffectRef.setEnabled(false);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "whip/skill2";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, true, 1.0F);
      player.target2.equipWhip();
      player.target2.playAnimation("skill2", true, 1.0F);
      AudioManager.playSound("whip_skill_2", player.getCenterPosition());
   }
}
