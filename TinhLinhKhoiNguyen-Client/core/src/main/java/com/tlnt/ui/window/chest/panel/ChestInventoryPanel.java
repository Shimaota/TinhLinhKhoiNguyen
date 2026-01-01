package com.tlnt.ui.window.chest.panel;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;
import com.tlnt.ui.window.charinfo.ItemCountGridPanel;

public final class ChestInventoryPanel extends GamePanel {
   public ItemCountGridPanel itemCountGrid;
   public InventoryGridPanel inventoryGrid;

   public ChestInventoryPanel(RenderContext ctx) {
      super(ctx);
      this.addActor(this.itemCountGrid = new ItemCountGridPanel(super.renderContext));
      this.addActor(this.inventoryGrid = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_4));
      this.inventoryGrid.setPosition(1247.0F, 0.0F, 20);
   }
}
