package com.tlnt.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.shimaota.a.a.Message;

public final class ServerListener extends Listener {
   private final INetworkClient networkClient;

   public ServerListener(INetworkClient networkClient) {
      this.networkClient = networkClient;
   }

   @Override
   public void connected(Connection connection) {
   }

   @Override
   public void received(Connection connection, Object receivedObject) {
      if (receivedObject instanceof DataPacket) {
         DataPacket packet = (DataPacket)receivedObject;
         MessageHandler.getInstance().receiveMessage(new Message(packet.opcode, packet.payload));
      } else if (receivedObject instanceof FragmentPacket) {
         MessageHandler.getInstance().receiveFragment((FragmentPacket)receivedObject);
      }
   }

   @Override
   public void disconnected(Connection connection) {
      networkClient.onDisconnected();
   }
}
