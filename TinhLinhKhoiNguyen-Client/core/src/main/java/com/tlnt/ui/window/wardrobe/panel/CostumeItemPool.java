package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class CostumeItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.wardrobe.panel.CostumePanel.EquipGridPanel equipGridPanel;
   public ButtonGroup<Button> buttonGroup;

   public CostumeItemPool(CostumePanel.EquipGridPanel panel) {
      super();
      this.equipGridPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.equipGridPanel.renderContext)).addListener(new CostumeItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
