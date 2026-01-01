package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class PrivateChatContactClickListener extends ClickListener {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel privateChatPanel;
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel.ContactRow contactRow;
   public PrivateChatContactClickListener(PrivateChatPanel.ContactRow contactRow, PrivateChatPanel privateChatPanel) {
      super();
      this.contactRow = contactRow;
      this.privateChatPanel = privateChatPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.contactRow.contact != null) {
         this.contactRow.privateChatPanel.selectContact(this.contactRow.contact);
      }
   }
}
