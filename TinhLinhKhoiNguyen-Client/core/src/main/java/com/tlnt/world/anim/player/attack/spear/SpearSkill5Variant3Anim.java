package com.tlnt.world.anim.player.attack.spear;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.spear.SpearProjectileEntity;
import com.tlnt.world.projectile.spear.component.SpearProjectileSkeleton;

public final class SpearSkill5Variant3Anim extends AnimationBehavior<PlayerEntity> {
   public static final SpearSkill5Variant3Anim INSTANCE = new SpearSkill5Variant3Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill25AttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.1F) {
            SpearProjectileEntity spear;
            SpearProjectileSkeleton spearSkeleton = (spear = SpearProjectileEntity.spawn(player.getCenterPosition().x, player.getCenterPosition().y)
                  .setTarget(player.skeleton.skill25AttackTarget)
                  .setOwner(player))
               .POOL2;
            String[] skins = new String[1];
            skins[0] = player.POOL2.weaponTexture;
            spearSkeleton.setSkins(skins);
            String animName = "spear2";
            spear.POOL2.playAnimation(animName, true, 1.0F);
            player.skeleton.skill25AttackTarget = null;
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "spear/skill5_3";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.0F);
      player.target2.equipSpear();
      animName = "skill5_3";
      player.target2.playAnimation(animName, false, 1.0F);
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
