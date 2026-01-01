package com.tlnt.ui.window.trade.panel;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class TradeBackpackPanel extends GamePanel {
   public EquipmentPaperdollPanel equipmentPanel;
   public InventoryGridPanel inventoryPanel;

   public TradeBackpackPanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.equipmentPanel = new EquipmentPaperdollPanel(super.renderContext));
      this.addActor(this.inventoryPanel = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_6));
      this.inventoryPanel.setPosition(1247.0F, 0.0F, 20);
   }
}
