package com.tlnt.ui.window.party.tab;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.party.panel.PartyBuffInfoPanel;
import com.tlnt.ui.window.party.panel.PartyNearbyListPanel;

public final class PartyJoinTab extends GamePanel {
   public PartyJoinTab(RenderContext renderContext) {
      super(renderContext);
      PartyBuffInfoPanel buffInfoPanel;
      (buffInfoPanel = new PartyBuffInfoPanel(super.renderContext)).setSize(490.0F, 610.0F);
      this.addActor(buffInfoPanel);
      PartyNearbyListPanel nearbyListPanel;
      (nearbyListPanel = new PartyNearbyListPanel(super.renderContext)).setSize(742.0F, 610.0F);
      nearbyListPanel.setPosition(505.0F, 0.0F);
      this.addActor(nearbyListPanel);
   }
}
