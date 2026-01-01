package com.tlnt.model.server;

public final class ServerInfo {
   public String name;
   public String host;
   public int port;

   public ServerInfo(String name, String host, int port) {
      this.name = name;
      this.host = host;
      this.port = port;
   }
}
