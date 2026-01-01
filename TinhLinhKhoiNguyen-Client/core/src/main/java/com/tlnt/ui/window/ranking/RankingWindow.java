package com.tlnt.ui.window.ranking;

import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.ranking.RankingCategory;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.ranking.panel.RankCategoryPanel;

public final class RankingWindow extends TabbedWindow {
   public RankingWindow(RenderContext ctx) {
      super(ctx, WindowType.RANK_POPUP);
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onOpen() {
      super.onOpen();
      if (this.getTabCount() == 0) {
         for (RankingCategory category : GameState.rankingCategories) {
            this.addTab(category.name, category.description, new RankCategoryPanel(super.renderContext, category));
         }

         if (this.getTabCount() != 0) {
            ServerPacketSender.getInstance().sendRequestRanking(GameState.rankingCategories.first().categoryId);
         }
      }
   }
}
