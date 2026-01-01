package com.tlnt.world.entity.player.component;

import com.tlnt.core.GameState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public final class MoveToTargetAndAttackCallback implements PlayerMovementController.MovementCallback {
   public com.tlnt.world.entity.player.component.InteractionController interactionController;
   public MoveToTargetAndAttackCallback(InteractionController interactionController) {
      super();
      this.interactionController = interactionController;
   }

   @Override
   public final void execute() {
      if (this.interactionController.speed != null && !this.interactionController.speed.isDead()) {
         EntityType targetType;
         if ((targetType = this.interactionController.speed.getEntityType()) == EntityType.PLAYER
            && !this.interactionController.projectile.autoTargetController.canTargetPlayer((PlayerEntity)this.interactionController.speed)) {
            this.interactionController.speed = null;
         } else if (!this.interactionController
            .projectile
            .autoTargetController
            .canUseSkill(GameState.selectedSkill, true)) {
            this.interactionController.speed = null;
         } else {
            ServerPacketSender sender = ServerPacketSender.getInstance();
            int targetKind;
            if (targetType == EntityType.MOB) {
               targetKind = 0;
            } else if (targetType == EntityType.PLAYER) {
               targetKind = 1;
            } else {
               targetKind = -1;
            }

            faceTarget(this.interactionController.projectile, this.interactionController.speed);
            sender.sendSkillbarAction(GameState.selectedSkill.skill.id, targetKind, this.interactionController.speed.getId());
            this.interactionController.speed = null;
         }
      } else {
         this.interactionController.speed = null;
      }
   }

   @Override
   public final boolean isAtDestination() {
      if (this.interactionController.speed == null || GameState.selectedSkill == null || GameState.selectedSkill.levelData == null) {
         return true; // target / skill lost on the way: stop, execute() skips by itself
      }
      return AttackReach.inReach(this.interactionController.projectile, this.interactionController.speed, GameState.selectedSkill.levelData);
   }

   /** Target cannot be reached (stuck): drop the pending attack, do NOT attack anyway (attacking out of reach only turns the character uselessly). */
   public final void cancel() {
      this.interactionController.speed = null;
   }

   /**
    * Turns to face the target once before casting. Skipped when at almost the same x: the sign of dx is then just noise (the mob
    * sways) -> a useless flip, and the facing-based camera look-ahead would jerk.
    */
   private static void faceTarget(PlayerEntity player, WorldEntity target) {
      if (Math.abs(target.getPosition().x - player.getPosition().x) > FACE_DEAD_ZONE) {
         player.faceTowards(target.getPosition());
      }
   }

   private static final float FACE_DEAD_ZONE = 0.1F;
}
