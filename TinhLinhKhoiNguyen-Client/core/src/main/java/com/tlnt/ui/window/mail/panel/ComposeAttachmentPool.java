package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class ComposeAttachmentPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.mail.panel.MailComposePanel composePanel;
   public ButtonGroup<Button> buttonGroup;

   public ComposeAttachmentPool(MailComposePanel composePanel) {
      super();
      this.composePanel = composePanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.composePanel.renderContext)).addListener(new AttachItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
