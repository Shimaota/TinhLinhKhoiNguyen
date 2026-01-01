package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.projectile.bow.component.BowArrowSkeletonComponent;

public final class BowSkill3Anim extends AnimationBehavior<PlayerEntity> {
   public static final BowSkill3Anim INSTANCE = new BowSkill3Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("bow/skill3")) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).movementComponent.getAnimationProgress(0) >= 0.5F) {
         if (player.skeleton.skill11MainTarget != null) {
            BowArrowProjectile arrowRef = BowArrowProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
               .setTarget(player.skeleton.skill11MainTarget)
               .setOwner(player)
               .setHitSoundName("hit2");
            BowArrowProjectile arrow;
            BowArrowSkeletonComponent arrowSkeleton = (arrow = arrowRef.setShakesCamera(false)).POOL2;
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            arrowSkeleton.setSkins(skins);
            String animName = "arrow_2";
            arrow.POOL2.playAnimation(animName, true, 1.5F);
            player.skeleton.skill11MainTarget = null;
         }

         if (!player.skeleton.skill11AoeTargets.isEmpty()) {
            for (GameObject target : player.skeleton.skill11AoeTargets) {
               BowArrowProjectile aoeArrowRef = BowArrowProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y).setTarget(target).setOwner(player);
               BowArrowProjectile aoeArrow;
               BowArrowSkeletonComponent aoeArrowSkeleton = (aoeArrow = aoeArrowRef.setHitSoundName("hit4")
                     .setShakesCamera(false))
                  .POOL2;
               String[] aoeSkins = new String[1];
               aoeSkins[0] = player.POOL2.weaponTexture;
               aoeArrowSkeleton.setSkins(aoeSkins);
               aoeArrow.POOL2.playAnimation("arrow_1", true, 1.5F);
               player.skeleton.skill11MainTarget = null;
            }

            player.skeleton.skill11AoeTargets.clear();
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("bow/skill3", false, 1.2F);
      player.target2.equipBow();
      player.target2.playAnimation("skill3", false, 1.2F);
      AudioManager.playSound("bow_skill_3", player.getCenterPosition());
   }
}
