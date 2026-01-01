package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class ItemCountSlotButtonPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.charinfo.ItemCountGridPanel itemCountGridPanel;
   public ButtonGroup<Button> buttonGroup;

   public ItemCountSlotButtonPool(ItemCountGridPanel panel) {
      super();
      this.itemCountGridPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton = new ItemSlotButton(this.itemCountGridPanel.renderContext);
      this.buttonGroup.add(slotButton);
      slotButton.addListener(new ItemSlotClickListener(this, slotButton));
      return slotButton;
   }
}
