package com.tlnt.ui.window.ranking.panel;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.ScrollPaneFactory;

public final class RankScrollRefreshListener implements ScrollPaneFactory.RefreshListener {
   public com.tlnt.ui.window.ranking.panel.RankCategoryPanel panel;
   public RankScrollRefreshListener(RankCategoryPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void onRefresh() {
      if (this.panel.category != null) {
         ServerPacketSender.getInstance().sendRequestRanking(this.panel.category.categoryId);
      }
   }
}
