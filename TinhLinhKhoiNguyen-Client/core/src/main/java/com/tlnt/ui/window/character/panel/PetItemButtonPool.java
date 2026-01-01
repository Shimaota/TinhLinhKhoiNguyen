package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class PetItemButtonPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.character.panel.PetPanel.PetEquipPanel petEquipPanel;
   public ButtonGroup<Button> buttonGroup;

   public PetItemButtonPool(PetPanel.PetEquipPanel panel) {
      super();
      this.petEquipPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.petEquipPanel.renderContext)).addListener(new CostumeItemMenuClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
