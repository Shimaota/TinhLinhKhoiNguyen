package com.tlnt.net;

import com.shimaota.a.a.IMessage;

public interface INetworkClient {
   boolean isConnected();
   boolean isConnecting();
   void connectAndSetup();
   void sendTcp(IMessage message);
   void disconnect();
   void switchServer();
   void onDisconnected();
}
