package com.tlnt.ui.window.market;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.market.panel.MarketListingRow;
import com.tlnt.ui.window.market.panel.MarketShopPanel;

public class MarketWindow extends TabbedWindow {

   public MarketWindow(RenderContext ctx) {
      super(ctx, WindowType.SHOP_CONSIGNMENT_POPUP);
      this.addTabWithCallback(
         "Ký gửi",
         "icon/282",
         new MarketListingRow(super.renderContext),
         () -> true
      );
      String tabTitle = "Cửa hàng";
      this.addTab(tabTitle, "icon/281", new MarketShopPanel(super.renderContext));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onOpen() {
      super.onOpen();
      ServerPacketSender.getInstance().sendRequestStats();
   }

}
