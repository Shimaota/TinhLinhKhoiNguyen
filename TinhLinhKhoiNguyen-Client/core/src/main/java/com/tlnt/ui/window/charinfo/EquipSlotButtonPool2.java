package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class EquipSlotButtonPool2 extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel ownerPanel;
   public ButtonGroup<Button> buttonGroup;

   public EquipSlotButtonPool2(EquipmentPaperdollPanel panel) {
      super();
      this.ownerPanel = panel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton = new ItemSlotButton(this.ownerPanel.renderContext);
      this.buttonGroup.add(slotButton);
      slotButton.addListener(new EquipSlotClickListener(this, slotButton));
      return slotButton;
   }
}
