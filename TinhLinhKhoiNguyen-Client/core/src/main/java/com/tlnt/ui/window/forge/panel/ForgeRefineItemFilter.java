package com.tlnt.ui.window.forge.panel;

import com.tlnt.model.item.Item;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeRefineItemFilter implements InventoryGridPanel.ItemFilter {
   public com.tlnt.ui.window.forge.panel.ForgeRefinePanel forgeRefinePanel;
   public ForgeRefineItemFilter(ForgeRefinePanel panel) {
      super();
      this.forgeRefinePanel = panel;
   }

   @Override
   public final boolean isItemAccepted(Item item) {
      if (item.template.id != 274) {
         if (item.template.type.id != 12) {
            return false;
         }
      }

      return true;
   }
}
