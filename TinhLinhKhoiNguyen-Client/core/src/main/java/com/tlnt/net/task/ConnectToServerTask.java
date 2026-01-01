package com.tlnt.net.task;

import com.tlnt.util.GameUtils;

public final class ConnectToServerTask implements Runnable {
   public int serverIndex;
   public com.tlnt.net.task.ServerPacketSender sender;
   public ConnectToServerTask(ServerPacketSender sender, int delayMillis) {
      super();
      this.sender = sender;
      this.serverIndex = delayMillis;
   }

   @Override
   public final void run() {
      GameUtils.sleepMillis(this.serverIndex);
      this.sender.networkClient.connectAndSetup();
   }
}
