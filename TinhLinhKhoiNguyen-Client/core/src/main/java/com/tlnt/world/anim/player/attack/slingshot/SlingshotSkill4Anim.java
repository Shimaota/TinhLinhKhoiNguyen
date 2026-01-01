package com.tlnt.world.anim.player.attack.slingshot;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;
import com.tlnt.world.projectile.slingshot.component.SlingshotSkeletonComponent;

public final class SlingshotSkill4Anim extends AnimationBehavior<PlayerEntity> {
   public static final SlingshotSkill4Anim INSTANCE = new SlingshotSkill4Anim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill18AttackTarget != null && player.movementComponent.getAnimationProgress(0) >= 0.3F) {
         SlingshotProjectile projectileRef = SlingshotProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y + player.getHeight() / 2.0F)
            .setTarget(player.skeleton.skill18AttackTarget)
            .setShooter(player);
         SlingshotProjectile projectile;
         SlingshotSkeletonComponent projectileSkeleton = (projectile = projectileRef.setDamageText("hit2").setScreenShake(false)).POOL2;
         String[] skins = new String[1];
         skins[0] = player.POOL2.weaponTexture;
         projectileSkeleton.setSkins(skins);
         projectile.POOL2.playAnimation("rock_2", true, 1.5F);
         player.skeleton.skill18AttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("slingshot/skill4", false, 1.15F);
      player.target2.equipSlingshot();
      String animName = "skill4";
      player.target2.playAnimation(animName, false, 1.15F);
      AudioManager.playSound("slingshot_skill_4", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
      WeaponEquipEffect weaponEffect;
      (weaponEffect = player.target2).setEnabled(false);
   }
}
