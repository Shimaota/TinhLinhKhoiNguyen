package com.tlnt.ui.window.shop;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.shop.panel.ComboPackPanel;
import com.tlnt.ui.window.shop.panel.LuckyWheelPanel;
import com.tlnt.ui.window.shop.panel.WeeklyTopPanel;

public final class ShopWindow extends TabbedWindow {

   public ShopWindow(RenderContext renderContext) {
      super(renderContext, WindowType.EVENT_POPUP);
      this.addTab("Top tuần", "icon/789", new WeeklyTopPanel(super.renderContext));
      this.addTab("Gói combo", "icon/753", new ComboPackPanel(super.renderContext));
      this.addTab("Vòng quay", "icon/752", new LuckyWheelPanel(super.renderContext));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

}
