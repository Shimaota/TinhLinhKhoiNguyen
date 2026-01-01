package com.tlnt.ui.window.forge.panel;

import com.tlnt.model.item.Item;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeElementItemFilter implements InventoryGridPanel.ItemFilter {
   public com.tlnt.ui.window.forge.panel.ForgeElementPanel panel;
   public ForgeElementItemFilter(ForgeElementPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final boolean isItemAccepted(Item item) {
      switch (item.template.type.id) {
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 13:
            return true;
         case 11:
         case 12:
         default:
            if (item.template.id == 171) {
               return true;
            } else {
               return false;
            }
      }
   }
}
