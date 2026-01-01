package com.tlnt.ui.window.ranking.panel;

import com.badlogic.gdx.utils.Pool;

public final class RankEntryRowPool extends Pool<RankCategoryPanel.RankEntryRow> {
   public com.tlnt.ui.window.ranking.panel.RankCategoryPanel panel;
   public RankEntryRowPool(RankCategoryPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public RankCategoryPanel.RankEntryRow newObject() {
      return (this.panel).new RankEntryRow(this.panel.renderContext);
   }
}
