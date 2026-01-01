package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;

public final class PartyJoinRequestRowPool extends Pool<PartyJoinRequestsPanel.JoinRequestRow> {
   public com.tlnt.ui.window.party.panel.PartyJoinRequestsPanel panel;
   public PartyJoinRequestRowPool(PartyJoinRequestsPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public PartyJoinRequestsPanel.JoinRequestRow newObject() {
      return (this.panel).new JoinRequestRow(this.panel.renderContext);
   }
}
