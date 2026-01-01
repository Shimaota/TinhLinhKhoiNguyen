package com.tlnt.world.anim.player.attack.axe;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.axe.AxeProjectileEntity;
import com.tlnt.world.projectile.axe.component.AxeSkeletonComponent;

public final class AxeSkill4Anim extends AnimationBehavior<PlayerEntity> {
   public static final AxeSkill4Anim INSTANCE = new AxeSkill4Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("axe/skill4")) {
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
      if ((player = (PlayerEntity)entity).skeleton.skill6AttackTarget != null
         && player.movementComponent.getAnimationProgress(0) >= 0.5F) {
         AxeProjectileEntity projectile;
         (projectile = AxeProjectileEntity.create(player.getCenterPosition().x, player.getCenterPosition().y).setTarget(player.skeleton.skill6AttackTarget).setOwner(player))
            .setDirection(player.getDirection());
         AxeSkeletonComponent projectileSkeleton = projectile.POOL2;
         String[] skins = new String[1];
         skins[0] = player.POOL2.weaponTexture;
         projectileSkeleton.setSkins(skins);
         String animName = "axe";
         projectile.POOL2.playAnimation(animName, true, 1.5F);
         player.skeleton.skill6AttackTarget = null;
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "axe/skill4";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.3F);
      player.target2.equipAxe();
      animName = "skill4";
      player.target2.playAnimation(animName, false, 1.3F);
      AudioManager.playSound("axe_skill_4", player.getCenterPosition());
   }
}
