package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;

public final class NearbyPartyRowPool extends Pool<PartyNearbyListPanel.NearbyPartyRow> {
   public com.tlnt.ui.window.party.panel.PartyNearbyListPanel panel;
   public NearbyPartyRowPool(PartyNearbyListPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public PartyNearbyListPanel.NearbyPartyRow newObject() {
      return (this.panel).new NearbyPartyRow(this.panel.renderContext);
   }
}
