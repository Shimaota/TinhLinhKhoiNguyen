package com.tlnt.ui.window.charinfo;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.player.PlayerStats;

public final class MobAttackStatRow extends CharacterStatRow {
   public CharacterStatsPanel entityStats;

   public MobAttackStatRow(CharacterStatsPanel statsPanel, RenderContext renderContext, String label) {
      super(renderContext, label);
      this.entityStats = statsPanel;
   }

   @Override
   public final String getStatValue(PlayerStats stats) {
      return CharacterStatsPanel.formatLong(stats.attackVsMob);
   }
}
