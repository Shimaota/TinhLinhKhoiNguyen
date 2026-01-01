package com.tlnt.world.anim.player.attack.slingshot;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;
import com.tlnt.world.projectile.slingshot.component.SlingshotSkeletonComponent;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class SlingshotSkill1Anim extends AnimationBehavior<PlayerEntity> {
   public static final SlingshotSkill1Anim INSTANCE = new SlingshotSkill1Anim();

   public static void spawnProjectile(PlayerEntity player) {
      SlingshotProjectile projectileRef = SlingshotProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
         .setTarget(player.skeleton.skill15AoeTargets.removeIndex(0))
         .setShooter(player)
         .setDamageText("hit4");
      SlingshotProjectile projectile;
      SlingshotSkeletonComponent projectileSkeleton = (projectile = projectileRef.setScreenShake(false)).POOL2;
      String[] skins = new String[1];
      skins[0] = player.POOL2.weaponTexture;
      projectileSkeleton.setSkins(skins);
      projectile.POOL2.playAnimation("rock_1", true, 1.5F);
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      int eventIndex = -1;
      switch (eventName.hashCode()) {
         case -1807429922:
            if (eventName.equals("slingshot/skill1_1")) {
               eventIndex = 0;
            }
            break;
         case -1807429921:
            if (eventName.equals("slingshot/skill1_2")) {
               eventIndex = 1;
            }
            break;
         case -1807429920:
            if (eventName.equals("slingshot/skill1_3")) {
               eventIndex = 2;
            }
      }

      switch (eventIndex) {
         case 0:
            if (player.skeleton.skill15AoeTargets.isEmpty()) {
               String finishAnimName = "slingshot/skill1_3";
               player.movementComponent.playAnimation(finishAnimName, false, 1.0F);
               player.target2.playAnimation("skill1_3", false, 1.0F);
               return;
            }

            spawnProjectile(player);
            String animName = "slingshot/skill1_2";
            player.movementComponent.playAnimation(animName, true, 2.0F);
            animName = "skill1_2";
            player.target2.playAnimation(animName, true, 2.0F);
            return;
         case 1:
            if (player.skeleton.skill15AoeTargets.isEmpty()) {
               player.movementComponent.playAnimation("slingshot/skill1_3", false, 1.0F);
               player.target2.playAnimation("skill1_3", false, 1.0F);
               return;
            }

            spawnProjectile(player);
            return;
         case 2:
            player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "slingshot/skill1_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.3F);
      player.target2.equipSlingshot();
      animName = "skill1_1";
      player.target2.playAnimation(animName, false, 1.3F);
      AudioManager.playSound("slingshot_skill_1", player.getCenterPosition());
   }
}
