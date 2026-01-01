package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.projectile.bow.component.BowArrowSkeletonComponent;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;

public final class BowSkill1Anim extends AnimationBehavior<PlayerEntity> {
   public static final BowSkill1Anim INSTANCE = new BowSkill1Anim();

   public static void spawnSkill1ArrowEffect(PlayerEntity player) {
      BowArrowProjectile arrowRef = BowArrowProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y);
      BowArrowProjectile arrow;
      BowArrowSkeletonComponent arrowSkeleton = (arrow = arrowRef.setTarget(player.skeleton.skill9HitTargets.removeIndex(0))
            .setOwner(player)
            .setHitSoundName("hit4")
            .setShakesCamera(false))
         .POOL2;
      String[] skins = new String[1];
      skins[0] = player.POOL2.weaponTexture;
      arrowSkeleton.setSkins(skins);
      String animName = "arrow_1";
      arrow.POOL2.playAnimation(animName, true, 1.5F);
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      int eventIndex = -1;
      switch (eventName.hashCode()) {
         case -946777513:
            if (eventName.equals("bow/skill1_1")) {
               eventIndex = 0;
            }
            break;
         case -946777512:
            if (eventName.equals("bow/skill1_2")) {
               eventIndex = 1;
            }
            break;
         case -946777511:
            if (eventName.equals("bow/skill1_3")) {
               eventIndex = 2;
            }
      }

      switch (eventIndex) {
         case 0:
            if (player.skeleton.skill9HitTargets.isEmpty()) {
               String animName = "bow/skill1_3";
               player.movementComponent.playAnimation(animName, false, 1.0F);
               animName = "skill1_3";
               player.target2.playAnimation(animName, false, 1.0F);
               return;
            }

            spawnSkill1ArrowEffect(player);
            player.movementComponent.playAnimation("bow/skill1_2", true, 2.0F);
            player.target2.playAnimation("skill1_2", true, 2.0F);
            return;
         case 1:
            if (player.skeleton.skill9HitTargets.isEmpty()) {
               String finishAnimName = "bow/skill1_3";
               player.movementComponent.playAnimation(finishAnimName, false, 1.0F);
               player.target2.playAnimation("skill1_3", false, 1.0F);
               return;
            }

            spawnSkill1ArrowEffect(player);
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
      String animName = "bow/skill1_1";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.3F);
      player.target2.equipBow();
      player.target2.playAnimation("skill1_1", false, 1.3F);
      AudioManager.playSound("bow_skill_1", player.getCenterPosition());
   }
}
