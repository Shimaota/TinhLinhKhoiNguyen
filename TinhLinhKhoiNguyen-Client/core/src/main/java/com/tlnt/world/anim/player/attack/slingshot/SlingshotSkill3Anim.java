package com.tlnt.world.anim.player.attack.slingshot;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;
import com.tlnt.world.projectile.slingshot.component.SlingshotSkeletonComponent;

public final class SlingshotSkill3Anim extends AnimationBehavior<PlayerEntity> {
   public static final SlingshotSkill3Anim INSTANCE = new SlingshotSkill3Anim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill17AttackTarget != null && player.movementComponent.getAnimationProgress(0) >= 0.5F) {
         SlingshotProjectile projectileRef = SlingshotProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
            .setTarget(player.skeleton.skill17AttackTarget)
            .setShooter(player);
         projectileRef = projectileRef.setDamageText("hit8");
         SlingshotProjectile projectile;
         SlingshotSkeletonComponent projectileSkeleton = (projectile = projectileRef.setScreenShake(true)).POOL2;
         String[] skins = new String[1];
         skins[0] = player.POOL2.weaponTexture;
         projectileSkeleton.setSkins(skins);
         projectile.POOL2.playAnimation("rock_2", true, 1.5F);
         player.skeleton.skill17AttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("slingshot/skill3", false, 1.2F);
      player.target2.equipSlingshot();
      player.target2.playAnimation("skill3", false, 1.2F);
      AudioManager.playSound("slingshot_skill_3", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
      WeaponEquipEffect weaponEffect;
      WeaponEquipEffect weaponEffectRef = weaponEffect = player.target2;
      weaponEffectRef.setEnabled(false);
   }
}
