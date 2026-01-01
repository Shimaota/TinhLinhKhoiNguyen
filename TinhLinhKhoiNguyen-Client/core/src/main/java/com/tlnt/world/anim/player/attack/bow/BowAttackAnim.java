package com.tlnt.world.anim.player.attack.bow;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.projectile.bow.component.BowArrowSkeletonComponent;

public final class BowAttackAnim extends AnimationBehavior<PlayerEntity> {
   public static final BowAttackAnim INSTANCE = new BowAttackAnim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("bow/attack")) {
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
      if ((player = (PlayerEntity)entity).skeleton.skill8AttackTarget != null
         && player.movementComponent.getAnimationProgress(0) >= 0.7F) {
         BowArrowProjectile arrowRef = BowArrowProjectile.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
            .setTarget(player.skeleton.skill8AttackTarget)
            .setOwner(player);
         arrowRef = arrowRef.setHitSoundName("hit1");
         BowArrowProjectile arrow;
         BowArrowSkeletonComponent arrowSkeleton = (arrow = arrowRef.setShakesCamera(false)).POOL2;
         String[] skins = new String[1];
         skins[0] = player.POOL2.weaponTexture;
         arrowSkeleton.setSkins(skins);
         arrow.POOL2.playAnimation("arrow_1", true, 1.5F);
         player.skeleton.skill8AttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "bow/attack";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.25F);
      player.target2.equipBow();
      player.target2.playAnimation("attack", false, 1.25F);
      AudioManager.playSound("bow_attack_1", player.getCenterPosition());
   }
}
