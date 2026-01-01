package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class ComposeAttachmentItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public ButtonGroup<ItemSlotButton> buttonGroup;

   public ComposeAttachmentItemPool(MailMessageRow mailMessageRow) {
      super();
      this.mailMessageRow = mailMessageRow;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton = new ItemSlotButton(this.mailMessageRow.renderContext);
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
