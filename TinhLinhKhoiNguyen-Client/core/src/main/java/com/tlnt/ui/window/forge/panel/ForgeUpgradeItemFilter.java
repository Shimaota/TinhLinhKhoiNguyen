package com.tlnt.ui.window.forge.panel;

import com.tlnt.model.item.Item;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeUpgradeItemFilter implements InventoryGridPanel.ItemFilter {
   public com.tlnt.ui.window.forge.panel.ForgeUpgradePanel panel;
   public ForgeUpgradeItemFilter(ForgeUpgradePanel panel) {
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
         case 12:
         case 17:
            return true;
         case 11:
         case 13:
         case 14:
         case 15:
         case 16:
         default:
            return false;
      }
   }
}
