package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.projectile.bow.component.BowArrowSkeletonComponent;

public final class BowSkill4Anim extends AnimationBehavior<PlayerEntity> {
   public static final BowSkill4Anim INSTANCE = new BowSkill4Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("bow/skill4")) {
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
      if ((player = (PlayerEntity)entity).skeleton.skill12AttackTarget != null
         && player.movementComponent.getAnimationProgress(0) >= 0.3F) {
         BowArrowProjectile arrowRef = BowArrowProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y + player.getHeight() / 2.0F)
            .setTarget(player.skeleton.skill12AttackTarget)
            .setOwner(player);
         BowArrowProjectile arrow;
         BowArrowSkeletonComponent arrowSkeleton = (arrow = arrowRef.setHitSoundName("hit4")
               .setShakesCamera(false))
            .POOL2;
         String[] skins = new String[1];
         skins[0] = player.POOL2.weaponTexture;
         arrowSkeleton.setSkins(skins);
         arrow.POOL2.playAnimation("arrow_2", true, 1.5F);
         player.skeleton.skill12AttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "bow/skill4";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.2F);
      player.target2.equipBow();
      animName = "skill4";
      player.target2.playAnimation(animName, false, 1.2F);
      AudioManager.playSound("bow_skill_4", player.getCenterPosition());
   }
}
