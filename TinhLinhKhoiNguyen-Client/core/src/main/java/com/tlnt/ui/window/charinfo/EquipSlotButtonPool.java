package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class EquipSlotButtonPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.charinfo.InspectEquipmentPanel inspectEquipmentPanel;
   public ButtonGroup<Button> buttonGroup;

   public EquipSlotButtonPool(InspectEquipmentPanel panel) {
      super();
      this.inspectEquipmentPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton = new ItemSlotButton(this.inspectEquipmentPanel.renderContext);
      this.buttonGroup.add(slotButton);
      slotButton.addListener(new InspectEquipSlotClickListener(this, slotButton));
      return slotButton;
   }
}
