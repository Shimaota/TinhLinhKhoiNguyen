package com.tlnt.ui.window.party.tab;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.party.panel.PartyBuffInfoPanel;
import com.tlnt.ui.window.party.panel.PartyNearbyPlayersPanel;

public final class PartyNearbyTab extends GamePanel {
   public PartyNearbyTab(RenderContext renderContext) {
      super(renderContext);
      PartyBuffInfoPanel buffInfoPanel;
      (buffInfoPanel = new PartyBuffInfoPanel(super.renderContext)).setSize(490.0F, 610.0F);
      this.addActor(buffInfoPanel);
      PartyNearbyPlayersPanel nearbyPlayersPanel;
      (nearbyPlayersPanel = new PartyNearbyPlayersPanel(super.renderContext)).setSize(742.0F, 610.0F);
      nearbyPlayersPanel.setPosition(505.0F, 0.0F);
      this.addActor(nearbyPlayersPanel);
   }
}
