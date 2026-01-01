package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.core.Settings;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.WindowType;

public final class SelectServerListener extends ClickListener {
   public com.tlnt.ui.dialog.ServerListDialog serverListDialog;
   public com.tlnt.ui.dialog.ServerListDialog.ServerItem serverItem;
   public SelectServerListener(ServerListDialog.ServerItem serverItem, ServerListDialog serverListDialog) {
      super();
      this.serverItem = serverItem;
      this.serverListDialog = serverListDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Settings.saveSelectedServer(this.serverItem.serverData.name, this.serverItem.serverData.host, this.serverItem.serverData.port);
      ServerPacketSender.getInstance().switchServer();
      GameMain.instance.getGameScreen().active.showDialog(WindowType.LOGIN_PANEL);
   }
}
