package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameState;

public final class PrivateChatRemoveContactListener extends ClickListener {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel privateChatPanel;
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel.ContactRow contactRow;
   public PrivateChatRemoveContactListener(PrivateChatPanel.ContactRow contactRow, PrivateChatPanel privateChatPanel) {
      super();
      this.contactRow = contactRow;
      this.privateChatPanel = privateChatPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.contactRow.contact != null) {
         GameState.chatManager.privateConversations.removeValue(this.contactRow.contact, true);
         GameState.chatManager.privateDirty = true;
         this.contactRow.privateChatPanel.refresh();
      }
   }
}
