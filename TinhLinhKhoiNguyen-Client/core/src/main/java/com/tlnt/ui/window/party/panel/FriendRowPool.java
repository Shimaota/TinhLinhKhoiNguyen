package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;

public final class FriendRowPool extends Pool<FriendListPanel.FriendRow> {
   public com.tlnt.ui.window.party.panel.FriendListPanel panel;
   public FriendRowPool(FriendListPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public FriendListPanel.FriendRow newObject() {
      return (this.panel).new FriendRow(this.panel.renderContext);
   }
}
