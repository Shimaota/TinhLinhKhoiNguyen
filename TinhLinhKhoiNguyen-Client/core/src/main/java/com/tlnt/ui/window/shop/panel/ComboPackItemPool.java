package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class ComboPackItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.shop.panel.ComboPackPanel.ComboPackRow comboPackRow;
   public ButtonGroup<Button> buttonGroup;

   public ComboPackItemPool(ComboPackPanel.ComboPackRow row) {
      super();
      this.comboPackRow = row;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.comboPackRow.comboPackPanel.renderContext)).addListener(new ComboPackItemBuyListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
