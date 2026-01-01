package com.tlnt.net.task;

import com.tlnt.core.GameDataLoader;
import com.tlnt.util.HttpClient;

public final class LoadGameDataTask implements Runnable {
   public com.tlnt.net.task.ServerPacketHandler handler;
   public java.lang.String url;
   public LoadGameDataTask(ServerPacketHandler handler, String url) {
      super();
      this.handler = handler;
      this.url = url;
   }

   @Override
   public final void run() {
      GameDataLoader.loadGameData(
         HttpClient.downloadBytes(this.url, 3, null, null), true
      );
   }
}
