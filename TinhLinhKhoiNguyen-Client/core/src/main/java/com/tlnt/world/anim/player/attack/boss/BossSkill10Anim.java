package com.tlnt.world.anim.player.attack.boss;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.projectile.thunderbolt.ThunderboltProjectile;

public final class BossSkill10Anim extends AnimationBehavior<PlayerEntity> {
   public static final BossSkill10Anim INSTANCE = new BossSkill10Anim();

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity entity, float delta) {
      PlayerEntity player;
      if ((player = (PlayerEntity)entity).skeleton.skill30AttackTarget != null) {
         if (player.movementComponent.getAnimationProgress(0) >= 0.7F) {
            String animName = "project";
            ThunderboltProjectile.obtain(player.getCenterPosition().x, player.getCenterPosition().y)
               .setTarget(player.skeleton.skill30AttackTarget)
               .POOL2
               .playAnimation(animName, true, 1.0F);
            player.skeleton.skill30AttackTarget = null;
         }
      }
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation("stick/skill10", false, 1.0F);
      AudioManager.playSound("axe_skill_4", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).setAnimationState(AnimationState.IDLE);
   }
}
