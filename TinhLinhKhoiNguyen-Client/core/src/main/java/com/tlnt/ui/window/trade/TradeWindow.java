package com.tlnt.ui.window.trade;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.trade.panel.TradeBackpackPanel;
import com.tlnt.ui.window.trade.panel.TradeOfferPanel;

public final class TradeWindow extends TabbedWindow {

   public TradeWindow(RenderContext renderContext) {
      super(renderContext, WindowType.TRANSACTION_POPUP);
      String tabLabel = "Giao dịch";
      this.addTab(tabLabel, "icon/282", new TradeOfferPanel(super.renderContext));
      tabLabel = "Hành trang";
      this.addTab(tabLabel, "icon/255", new TradeBackpackPanel(super.renderContext));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onClose() {
      ServerPacketSender.getInstance().sendRequestFishing();
   }

}
