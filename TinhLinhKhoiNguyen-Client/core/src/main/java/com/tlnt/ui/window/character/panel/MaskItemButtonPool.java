package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class MaskItemButtonPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.character.panel.MaskPanel.MaskEquipPanel maskEquipPanel;
   public ButtonGroup<Button> buttonGroup;

   public MaskItemButtonPool(MaskPanel.MaskEquipPanel panel) {
      super();
      this.maskEquipPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.maskEquipPanel.renderContext)).addListener(new PetEquipMenuClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
