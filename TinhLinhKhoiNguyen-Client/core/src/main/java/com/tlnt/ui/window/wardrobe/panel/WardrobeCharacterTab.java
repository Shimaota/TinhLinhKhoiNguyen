package com.tlnt.ui.window.wardrobe.panel;

import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.CharacterStatsPanel;
import com.tlnt.ui.window.charinfo.InspectEquipmentPanel;

public final class WardrobeCharacterTab extends GamePanel {
   public InspectEquipmentPanel equipmentPanel;
   public CharacterStatsPanel statsPanel;

   public WardrobeCharacterTab(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.equipmentPanel = new InspectEquipmentPanel(super.renderContext));
      this.addActor(this.statsPanel = new CharacterStatsPanel(super.renderContext));
      this.statsPanel.setPosition(1247.0F, 0.0F, 20);
   }

   @Override
   public final void refresh() {
      this.statsPanel.setEntityStats(GameState.wardrobeData.playerStats);
   }
}
