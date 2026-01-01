package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.projectile.bow.component.BowArrowSkeletonComponent;

public final class BowSkill5Variant3Anim extends AnimationBehavior<PlayerEntity> {
   public static final BowSkill5Variant3Anim INSTANCE = new BowSkill5Variant3Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("bow/skill5_3")) {
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
      if ((player = (PlayerEntity)entity).skeleton.skill13AttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.1F) {
            BowArrowProjectile arrowRef = BowArrowProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
               .setTarget(player.skeleton.skill13AttackTarget)
               .setOwner(player)
               .setHitSoundName("hit5");
            BowArrowProjectile arrow;
            BowArrowSkeletonComponent arrowSkeleton = (arrow = arrowRef.setShakesCamera(true)).POOL2;
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            arrowSkeleton.setSkins(skins);
            String animName = "arrow_3";
            arrow.POOL2.playAnimation(animName, true, 1.5F);
            player.skeleton.skill13AttackTarget = null;
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("bow/skill5_3", false, 1.15F);
      player.target2.equipBow();
      player.target2.playAnimation("skill5_3", false, 1.15F);
   }
}
