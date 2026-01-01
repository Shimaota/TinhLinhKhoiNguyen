package com.tlnt.net;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.shimaota.a.a.IMessage;
import com.tlnt.core.GameMain;
import com.tlnt.core.Settings;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.screen.game.GameScreen;
import com.tlnt.ui.window.WindowType;

public class KryoNetworkClient implements INetworkClient {
   public static boolean connected;
   public static boolean connecting;
   public Client kryoClient;
   /** true when the client itself called {@link #disconnect()} (server switch), so onDisconnected does not report an error. */
   private volatile boolean intentionalDisconnect;

   @Override
   public final boolean isConnected() {
      return this.kryoClient != null && this.kryoClient.isConnected();
   }

   @Override
   public final boolean isConnecting() {
      return connecting;
   }

   @Override
   public final synchronized void connectAndSetup() {
      if (!connecting) {
         connecting = true;

         new Thread(() -> {
            try {
               this.kryoClient = new Client(65536, 65536);
               this.kryoClient.start();
               this.kryoClient.getKryo().register(DataPacket.class, 75);
               this.kryoClient.getKryo().register(byte[].class, 74);
               this.kryoClient.connect(1000, Settings.getServerHost(), Settings.getServerPort());
               this.kryoClient.setKeepAliveTCP(8000);
               this.kryoClient.addListener(new ServerListener(this));
               connected = true;
               ServerPacketSender.getInstance().sendHandshake();
               // The first connect runs on its own thread; GameScreen/active may not be created yet at that point.
               // UI changes must run on the GL thread, not on this network thread.
               Gdx.app.postRunnable(() -> {
                  GameScreen gameScreen = GameMain.instance != null ? GameMain.instance.getGameScreen() : null;
                  if (gameScreen != null && gameScreen.active != null
                        && gameScreen.active.getActiveWindowType() == WindowType.DISCONNECTED_PANEL) {
                     gameScreen.active.showDialog(WindowType.LOGIN_PANEL);
                  }
               });

               connecting = false;
            } catch (Exception e) {
               e.printStackTrace();
               connecting = false;
               if (this.kryoClient != null) {
                  this.kryoClient.stop();
               }
               ServerPacketSender.getInstance().connectToServer(2000);
               Gdx.app.postRunnable(new ConnectionErrorTask(this));
            }
         }, "kryo-connect").start();
      }
   }

   @Override
   public void onDisconnected() {
      connected = false;
      if (this.intentionalDisconnect) {
         // Disconnected on purpose to switch servers (SelectServerListener): not an error, connect to the new server right away.
         this.intentionalDisconnect = false;
         System.out.println("doi may chu -> " + Settings.getServerHost() + ":" + Settings.getServerPort());
         ServerPacketSender.getInstance().connectToServer(0);
         return;
      }

      System.out.println("mat ket noi");
      ServerPacketSender.getInstance().connectToServer(1000);
      Gdx.app.postRunnable(new DisconnectHandler(this));
   }

   @Override
   public final void sendTcp(IMessage message) {
      if (this.isConnected()) {
         this.kryoClient.sendTCP(new DataPacket(message));
      }
   }

   @Override
   public final void disconnect() {
      if (this.isConnected()) {
         this.kryoClient.stop();
      }
   }

   /** Disconnects from the current server and connects to the one in Settings, without showing the "connection lost" dialog. */
   @Override
   public final void switchServer() {
      if (this.isConnected()) {
         this.intentionalDisconnect = true;
         this.kryoClient.stop();
      } else {
         // Never managed to connect to the old server: try the newly selected one right away.
         ServerPacketSender.getInstance().connectToServer(0);
      }
   }
}
