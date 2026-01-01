package com.tlnt.ui.window.charinfo;

import com.tlnt.model.item.Item;
import java.util.Comparator;

public final class InventoryItemComparator implements Comparator<Item> {
   public com.tlnt.ui.window.charinfo.InventoryGridPanel inventoryGridPanel;
   public InventoryItemComparator(InventoryGridPanel panel) {
      super();
      this.inventoryGridPanel = panel;
   }

   public final int compare(Item a, Item b) {
      if (a == null && b != null) {
         return 1;
      } else {
         return a != null && b == null ? -1 : 0;
      }
   }
}
