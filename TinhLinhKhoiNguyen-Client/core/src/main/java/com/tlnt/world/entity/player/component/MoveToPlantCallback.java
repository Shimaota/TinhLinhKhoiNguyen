package com.tlnt.world.entity.player.component;

import com.tlnt.net.task.ServerPacketSender;

public final class MoveToPlantCallback implements PlayerMovementController.MovementCallback {
   public com.tlnt.world.entity.player.component.InteractionController interactionController;
   public MoveToPlantCallback(InteractionController interactionController) {
      super();
      this.interactionController = interactionController;
   }

   @Override
   public final void execute() {
      ServerPacketSender.getInstance().sendGuildVaultAction(this.interactionController.targetPlant.getId());
   }

   @Override
   public final boolean isAtDestination() {
      return false;
   }
}
