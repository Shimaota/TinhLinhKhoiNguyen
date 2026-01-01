package com.tlnt.ui.window.forge.panel;

import com.tlnt.model.item.Item;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeBrewItemFilter implements InventoryGridPanel.ItemFilter {
   public com.tlnt.ui.window.forge.panel.ForgeBrewPanel panel;
   public ForgeBrewItemFilter(ForgeBrewPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final boolean isItemAccepted(Item item) {
      if (item.template.type.id == 28) {
         return true;
      } else {
         return ((item.template.id == 201 ? 1 : 0) != 0);
      }
   }
}
