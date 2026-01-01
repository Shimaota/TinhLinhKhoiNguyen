package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class TitleItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.talent.panel.TitlePanel.TitleListPanel titleListPanel;
   public ButtonGroup<Button> buttonGroup;

   public TitleItemPool(TitlePanel.TitleListPanel listPanel) {
      super();
      this.titleListPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.titleListPanel.renderContext)).addListener(new CostumeEquipClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
