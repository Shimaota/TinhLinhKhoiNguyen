package com.tlnt.net.task;

import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PickupItemTask implements Runnable {
   public com.tlnt.net.task.ServerPacketHandler handler;
   public com.tlnt.world.entity.mapitem.MapItemEntity mapItem;
   public com.tlnt.world.entity.player.PlayerEntity player;
   public PickupItemTask(ServerPacketHandler handler, MapItemEntity mapItem, PlayerEntity player) {
      super();
      this.handler = handler;
      this.mapItem = mapItem;
      this.player = player;
   }

   @Override
   public final void run() {
      this.mapItem.POOL.startPull(this.player);
   }
}
