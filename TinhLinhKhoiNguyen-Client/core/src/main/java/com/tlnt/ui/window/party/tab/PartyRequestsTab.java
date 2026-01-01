package com.tlnt.ui.window.party.tab;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.party.panel.PartyBuffInfoPanel;
import com.tlnt.ui.window.party.panel.PartyJoinRequestsPanel;

public final class PartyRequestsTab extends GamePanel {
   public PartyRequestsTab(RenderContext renderContext) {
      super(renderContext);
      PartyBuffInfoPanel buffInfoPanel;
      (buffInfoPanel = new PartyBuffInfoPanel(super.renderContext)).setSize(490.0F, 610.0F);
      this.addActor(buffInfoPanel);
      PartyJoinRequestsPanel joinRequestsPanel;
      (joinRequestsPanel = new PartyJoinRequestsPanel(super.renderContext)).setSize(742.0F, 610.0F);
      joinRequestsPanel.setPosition(505.0F, 0.0F);
      this.addActor(joinRequestsPanel);
   }
}
