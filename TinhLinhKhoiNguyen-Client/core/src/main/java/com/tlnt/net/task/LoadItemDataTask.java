package com.tlnt.net.task;

import com.tlnt.core.GameDataLoader;
import com.tlnt.util.HttpClient;

public final class LoadItemDataTask implements Runnable {
   public com.tlnt.net.task.ServerPacketHandler handler;
   public java.lang.String url;
   public LoadItemDataTask(ServerPacketHandler handler, String url) {
      super();
      this.handler = handler;
      this.url = url;
   }

   @Override
   public final void run() {
      byte[] data = HttpClient.downloadBytes(this.url, 3, null, null);
      GameDataLoader.loadItemData(data, true);
   }
}
