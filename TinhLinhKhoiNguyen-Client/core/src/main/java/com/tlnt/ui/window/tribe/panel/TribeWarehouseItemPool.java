package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class TribeWarehouseItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.tribe.panel.TribeWarehousePanel.ItemGridPanel ownerGridPanel;
   public ButtonGroup<Button> buttonGroup;

   public TribeWarehouseItemPool(TribeWarehousePanel.ItemGridPanel ownerGridPanel) {
      super();
      this.ownerGridPanel = ownerGridPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.ownerGridPanel.renderContext)).addListener(new TribeWarehouseItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
