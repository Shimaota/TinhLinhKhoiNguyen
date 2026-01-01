package com.tlnt.net;

public final class ConnectionErrorTask implements Runnable {
   public com.tlnt.net.INetworkClient networkClient;

   public ConnectionErrorTask(INetworkClient networkClient) {
      super();
      this.networkClient = networkClient;
   }

   @Override
   public final void run() {
      System.out.println("Có lỗi xảy ra khi kết nối tới máy chủ");
   }

}
