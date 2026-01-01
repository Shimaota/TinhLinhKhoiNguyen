package com.tlnt.ui.window.marketsell;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.consignment.ConsignmentCategory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.marketsell.panel.MarketSellItemRow;

public class MarketSellWindow extends TabbedWindow {
   public Pool<MarketSellItemRow> OVERLAY_COLOR = new MarketSellItemRowPool(this);
   public int contentGroup = -1;

   public MarketSellWindow(RenderContext ctx) {
      super(ctx, WindowType.SHOP_POPUP);
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onUpdate() {
      if (GameState.shopData.dirty) {
         Array.ArrayIterator panelIter = this.getPanels().iterator();

         while (panelIter.hasNext()) {
            GamePanel panel;
            if ((panel = (GamePanel)panelIter.next()) instanceof MarketSellItemRow) {
               this.OVERLAY_COLOR.free((MarketSellItemRow)panel);
            }
         }

         this.clearAllTabs();

         for (ConsignmentCategory category : GameState.shopData.categories) {
            this.addTab(category.tabName, category.tabNameAlt, this.OVERLAY_COLOR.obtain().setItemCategory(category));
         }

         GameState.shopData.dirty = false;
      }
   }
}
