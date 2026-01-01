package com.tlnt.world.entity.player.component;

import com.tlnt.net.task.ServerPacketSender;

public final class MoveToNpcCallback implements PlayerMovementController.MovementCallback {
   public com.tlnt.world.entity.player.component.InteractionController interactionController;
   public MoveToNpcCallback(InteractionController interactionController) {
      super();
      this.interactionController = interactionController;
   }

   @Override
   public final void execute() {
      ServerPacketSender.getInstance()
         .sendUnequipItem(this.interactionController.autoFlipNpc.getId(), this.interactionController.autoFlipNpc.POOL2.template.id);
   }

   @Override
   public final boolean isAtDestination() {
      return false;
   }
}
