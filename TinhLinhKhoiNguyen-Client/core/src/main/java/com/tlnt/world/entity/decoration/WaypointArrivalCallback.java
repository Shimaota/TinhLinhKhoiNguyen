package com.tlnt.world.entity.decoration;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.entity.player.component.PlayerMovementController;

public final class WaypointArrivalCallback implements PlayerMovementController.MovementCallback {
   public com.tlnt.world.entity.decoration.WaypointEntity waypointEntity;
   public WaypointArrivalCallback(WaypointEntity waypoint) {
      super();
      this.waypointEntity = waypoint;
   }

   @Override
   public final void execute() {
      ServerPacketSender.getInstance().sendPlayerAction(this.waypointEntity.POOL2);
   }

   @Override
   public final boolean isAtDestination() {
      return this.waypointEntity.containsPoint(PlayerEntity.getInstance().getPosition().x, PlayerEntity.getInstance().getPosition().y);
   }
}
