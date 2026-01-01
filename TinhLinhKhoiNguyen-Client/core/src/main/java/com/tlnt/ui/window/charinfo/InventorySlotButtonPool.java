package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class InventorySlotButtonPool extends Pool<ItemSlotButton> {
   public final com.tlnt.ui.window.charinfo.InventoryGridPanel ownerPanel;
   public ButtonGroup<Button> buttonGroup;

   public InventorySlotButtonPool(InventoryGridPanel panel) {
      this.ownerPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton = new ItemSlotButton(this.ownerPanel.renderContext);
      this.buttonGroup.add(slotButton);
      slotButton.addListener(new InventoryItemContextMenuListener(this, slotButton));
      return slotButton;
   }
}
