package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class TitleItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.wardrobe.panel.TitlePanel.SelectionPanel selectionPanel;
   public ButtonGroup<Button> buttonGroup;

   public TitleItemPool(TitlePanel.SelectionPanel panel) {
      super();
      this.selectionPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.selectionPanel.renderContext)).addListener(new TitleItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
