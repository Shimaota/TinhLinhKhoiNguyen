package com.tlnt.world.anim.player.attack.slingshot;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;
import com.tlnt.world.projectile.slingshot.component.SlingshotSkeletonComponent;

public final class SlingshotAttackAnim extends AnimationBehavior<PlayerEntity> {
   public static final SlingshotAttackAnim INSTANCE = new SlingshotAttackAnim();

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill14AttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.3F) {
            SlingshotProjectile projectileRef = SlingshotProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
               .setTarget(player.skeleton.skill14AttackTarget)
               .setShooter(player);
            SlingshotProjectile projectile;
            SlingshotSkeletonComponent projectileSkeleton = (projectile = projectileRef.setDamageText("hit1").setScreenShake(false)).POOL2;
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            projectileSkeleton.setSkins(skins);
            String animName = "rock_1";
            projectile.POOL2.playAnimation(animName, true, 1.5F);
            player.skeleton.skill14AttackTarget = null;
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "slingshot/attack";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipSlingshot();
      player.target2.playAnimation("attack", false, 1.0F);
      AudioManager.playSound("slingshot_attack", player.getCenterPosition());
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
