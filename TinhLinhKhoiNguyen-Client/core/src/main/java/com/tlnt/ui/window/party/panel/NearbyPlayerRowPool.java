package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;

public final class NearbyPlayerRowPool extends Pool<PartyNearbyPlayersPanel.NearbyPlayerRow> {
   public com.tlnt.ui.window.party.panel.PartyNearbyPlayersPanel panel;
   public NearbyPlayerRowPool(PartyNearbyPlayersPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public PartyNearbyPlayersPanel.NearbyPlayerRow newObject() {
      return (this.panel).new NearbyPlayerRow(this.panel.renderContext);
   }
}
