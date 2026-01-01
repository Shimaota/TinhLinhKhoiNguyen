package com.tlnt.ui.window.character.panel;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.CharacterStatsPanel;
import com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class CharacterStatsTab extends GamePanel {
   public EquipmentPaperdollPanel paperdollPanel;
   public CharacterStatsPanel statsPanel;

   public CharacterStatsTab(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.paperdollPanel = new EquipmentPaperdollPanel(super.renderContext));
      this.addActor(this.statsPanel = new CharacterStatsPanel(super.renderContext));
      this.statsPanel.setPosition(1247.0F, 0.0F, 20);
   }

   @Override
   public final void onShow() {
      if (PlayerEntity.getInstance() != null) {
         this.statsPanel.setEntityStats(PlayerEntity.getInstance().POOL2);
      }
   }
}
