package com.tlnt.ui.window.ranking;

import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.ranking.panel.RankCategoryPanel;

/**
 * Ranking sent separately by the server (op 75 — decompiled C3892 / C3755, {@code OTHER_RANK_POPUP}): one "Rank" tab, columns
 * "Rank | Character name | (column name sent by the server) | Character", no row for the local player. Data lives in {@link GameState#otherRanking}.
 */
public final class OtherRankWindow extends TabbedWindow {
   public OtherRankWindow(RenderContext ctx) {
      super(ctx, WindowType.OTHER_RANK_POPUP);
      this.addTab("Rank", "", new RankCategoryPanel(super.renderContext, GameState.otherRanking));
   }
}
