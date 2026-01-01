package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;

public final class PrivateChatContactRowPool extends Pool<PrivateChatPanel.ContactRow> {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel privateChatPanel;
   public PrivateChatContactRowPool(PrivateChatPanel privateChatPanel) {
      super();
      this.privateChatPanel = privateChatPanel;
   }

   @Override
   public PrivateChatPanel.ContactRow newObject() {
      return (this.privateChatPanel).new ContactRow(this.privateChatPanel.renderContext);
   }
}
