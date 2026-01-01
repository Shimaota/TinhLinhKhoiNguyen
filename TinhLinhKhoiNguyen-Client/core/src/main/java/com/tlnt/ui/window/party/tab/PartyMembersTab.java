package com.tlnt.ui.window.party.tab;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.party.panel.PartyBuffInfoPanel;
import com.tlnt.ui.window.party.panel.PartyMembersPanel;

public final class PartyMembersTab extends GamePanel {
   public PartyMembersTab(RenderContext renderContext) {
      super(renderContext);
      PartyBuffInfoPanel buffInfoPanel;
      (buffInfoPanel = new PartyBuffInfoPanel(super.renderContext)).setSize(490.0F, 610.0F);
      this.addActor(buffInfoPanel);
      PartyMembersPanel membersPanel;
      (membersPanel = new PartyMembersPanel(super.renderContext)).setSize(742.0F, 610.0F);
      membersPanel.setPosition(505.0F, 0.0F);
      this.addActor(membersPanel);
   }
}
