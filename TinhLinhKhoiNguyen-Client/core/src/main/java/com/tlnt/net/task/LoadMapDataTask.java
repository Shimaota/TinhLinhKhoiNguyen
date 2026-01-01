package com.tlnt.net.task;

import com.tlnt.core.GameDataLoader;
import com.tlnt.util.HttpClient;

public final class LoadMapDataTask implements Runnable {
   public com.tlnt.net.task.ServerPacketHandler handler;
   public java.lang.String url;
   public LoadMapDataTask(ServerPacketHandler handler, String url) {
      super();
      this.handler = handler;
      this.url = url;
   }

   @Override
   public final void run() {
      GameDataLoader.loadMapData(
         HttpClient.downloadBytes(this.url, 3, null, null), true
      );
   }
}
