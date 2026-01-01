package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.utils.Pool;

public final class TribePendingRowPool extends Pool<TribePendingPanel.PendingMemberRow> {
   public com.tlnt.ui.window.tribe.panel.TribePendingPanel ownerPanel;
   public TribePendingRowPool(TribePendingPanel ownerPanel) {
      super();
      this.ownerPanel = ownerPanel;
   }

   @Override
   public TribePendingPanel.PendingMemberRow newObject() {
      return (this.ownerPanel).new PendingMemberRow(this.ownerPanel.renderContext);
   }
}
