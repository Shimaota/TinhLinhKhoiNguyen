package com.shimaota;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.tlnt.core.GameMain;

public final class GameWindowListener extends Lwjgl3WindowAdapter {
   public GameMain gameMain;

   public GameWindowListener(GameMain var1) {
      super();
      this.gameMain = var1;
   }

   @Override
   public final void filesDropped(String[] var1) {
      GameMain.printArgs(var1);
   }
}
