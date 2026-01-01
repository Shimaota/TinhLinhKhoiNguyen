package com.tlnt.world.entity.player.component;

import com.tlnt.net.task.ServerPacketSender;

public final class MoveToItemCallback implements PlayerMovementController.MovementCallback {
   public com.tlnt.world.entity.player.component.InteractionController interactionController;
   public MoveToItemCallback(InteractionController interactionController) {
      super();
      this.interactionController = interactionController;
   }

   @Override
   public final void execute() {
      ServerPacketSender.getInstance().sendQuestAction(this.interactionController.active.getId());
   }

   @Override
   public final boolean isAtDestination() {
      return false;
   }
}
