package com.tlnt.ui.window.forge.panel;

import com.tlnt.model.item.Item;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeCraftItemFilter implements InventoryGridPanel.ItemFilter {
   public com.tlnt.ui.window.forge.panel.ForgeCraftPanel forgeCraftPanel;
   public ForgeCraftItemFilter(ForgeCraftPanel panel) {
      super();
      this.forgeCraftPanel = panel;
   }

   @Override
   public final boolean isItemAccepted(Item item) {
      switch (item.template.type.id) {
         case 33:
            return true;
         default:
            if (item.template.id != 171) {
               if (item.template.id != 255) {
                  return false;
               }
            }

            return true;
      }
   }
}
