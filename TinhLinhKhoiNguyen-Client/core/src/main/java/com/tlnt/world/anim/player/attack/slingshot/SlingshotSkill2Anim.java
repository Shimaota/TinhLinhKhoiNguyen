package com.tlnt.world.anim.player.attack.slingshot;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;

public final class SlingshotSkill2Anim extends AnimationBehavior<PlayerEntity> {
   public static final SlingshotSkill2Anim INSTANCE = new SlingshotSkill2Anim();

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "slingshot/skill2";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.25F);
      player.secondaryWeaponEffect.equipSlingshot();
      WeaponEquipEffect weaponEffect = player.secondaryWeaponEffect;
      String[] skins = new String[1];
      skins[0] = player.POOL2.weaponTexture;
      weaponEffect.setSkins(skins);
      animName = "skill2_1";
      player.secondaryWeaponEffect.playAnimation(animName, false, 1.25F);
      AudioManager.playSound("slingshot_skill_2", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
      player.secondaryWeaponEffect.playAnimation("skill2_2", true, 1.25F);
   }
}
