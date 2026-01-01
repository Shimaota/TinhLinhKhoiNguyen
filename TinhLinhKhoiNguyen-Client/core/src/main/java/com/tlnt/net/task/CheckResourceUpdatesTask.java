package com.tlnt.net.task;

import com.tlnt.core.GameDataLoader;

public final class CheckResourceUpdatesTask implements Runnable {
   public com.tlnt.net.task.ServerPacketHandler handler;
   public java.lang.String version;
   public java.lang.String baseUrl;
   public CheckResourceUpdatesTask(ServerPacketHandler handler, String baseUrl, String version) {
      super();
      this.handler = handler;
      this.baseUrl = baseUrl;
      this.version = version;
   }

   @Override
   public final void run() {
      GameDataLoader.checkAndDownloadResources(this.baseUrl, this.version);
   }
}
