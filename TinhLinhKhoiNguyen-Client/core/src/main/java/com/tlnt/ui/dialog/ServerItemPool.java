package com.tlnt.ui.dialog;

import com.badlogic.gdx.utils.Pool;

public final class ServerItemPool extends Pool<ServerListDialog.ServerItem> {
   public com.tlnt.ui.dialog.ServerListDialog serverListDialog;
   public ServerItemPool(ServerListDialog serverListDialog) {
      super();
      this.serverListDialog = serverListDialog;
   }

   @Override
   public ServerListDialog.ServerItem newObject() {
      return (this.serverListDialog).new ServerItem("", this.serverListDialog.renderContext);
   }
}
