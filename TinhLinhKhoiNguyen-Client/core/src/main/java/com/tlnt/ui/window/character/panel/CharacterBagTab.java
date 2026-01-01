package com.tlnt.ui.window.character.panel;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public class CharacterBagTab extends GamePanel {
   public EquipmentPaperdollPanel paperdollPanel;
   public InventoryGridPanel inventoryGridPanel;

   public CharacterBagTab(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.paperdollPanel = new EquipmentPaperdollPanel(super.renderContext));
      this.addActor(this.inventoryGridPanel = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_1));
      this.inventoryGridPanel.setPosition(1247.0F, 0.0F, 20);
   }
}
