package com.tlnt.world.entity.player.component;

import com.tlnt.net.task.ServerPacketSender;

public final class MoveToAnimalCallback implements PlayerMovementController.MovementCallback {
   public com.tlnt.world.entity.player.component.InteractionController interactionController;
   public MoveToAnimalCallback(InteractionController interactionController) {
      super();
      this.interactionController = interactionController;
   }

   @Override
   public final void execute() {
      ServerPacketSender.getInstance().sendHarvestAction(this.interactionController.projectile2.getId());
   }

   @Override
   public final boolean isAtDestination() {
      return false;
   }
}
