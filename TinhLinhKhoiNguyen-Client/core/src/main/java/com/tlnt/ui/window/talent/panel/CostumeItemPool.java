package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class CostumeItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.talent.panel.CostumePanel.CostumeListPanel costumeListPanel;
   public ButtonGroup<Button> buttonGroup;

   public CostumeItemPool(CostumePanel.CostumeListPanel listPanel) {
      super();
      this.costumeListPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.costumeListPanel.renderContext)).addListener(new SelectCostumeItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
