package com.tlnt.net;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.ui.window.WindowType;

public final class DisconnectHandler implements Runnable {
   public com.tlnt.net.INetworkClient networkClient;
   public DisconnectHandler(INetworkClient networkClient) {
      super();
      this.networkClient = networkClient;
   }

   @Override
   public final void run() {
      GameState.clearWorldEntities(true);
      GameMain.instance.showGameScreen();
      GameMain.instance.getGameScreen().active.showDialog(WindowType.DISCONNECTED_PANEL);
   }
}
