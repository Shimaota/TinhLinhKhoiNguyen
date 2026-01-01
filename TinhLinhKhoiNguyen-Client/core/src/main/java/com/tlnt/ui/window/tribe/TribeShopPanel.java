package com.tlnt.ui.window.tribe;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.marketsell.panel.MarketSellItemRow;

public final class TribeShopPanel extends MarketSellItemRow {
   public com.tlnt.ui.window.tribe.TribeWindow itemSlotButtonPool;
   public TribeShopPanel(TribeWindow tribeWindow, RenderContext renderContext) {
      super(renderContext);
      this.itemSlotButtonPool = tribeWindow;
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestEnhance();
   }
}
