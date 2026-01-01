package com.tlnt.ui.window.charinfo;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.player.PlayerStats;

public final class MpRegenStatRow extends CharacterStatRow {
   public CharacterStatsPanel entityStats;

   public MpRegenStatRow(CharacterStatsPanel statsPanel, RenderContext renderContext, String label) {
      super(renderContext, label);
      this.entityStats = statsPanel;
   }

   @Override
   public final String getStatValue(PlayerStats stats) {
      return CharacterStatsPanel.formatLong(stats.mpRegen);
   }
}
