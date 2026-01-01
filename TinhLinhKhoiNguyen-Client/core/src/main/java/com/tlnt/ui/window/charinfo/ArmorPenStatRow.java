package com.tlnt.ui.window.charinfo;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.player.PlayerStats;

public final class ArmorPenStatRow extends CharacterStatRow {
   public CharacterStatsPanel entityStats;

   public ArmorPenStatRow(CharacterStatsPanel statsPanel, RenderContext renderContext, String label) {
      super(renderContext, label);
      this.entityStats = statsPanel;
   }

   @Override
   public final String getStatValue(PlayerStats stats) {
      return stats.armorPen + "";
   }
}
