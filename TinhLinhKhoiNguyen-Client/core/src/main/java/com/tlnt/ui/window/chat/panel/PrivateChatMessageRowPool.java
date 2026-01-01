package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;

public final class PrivateChatMessageRowPool extends Pool<PrivateChatPanel.MessageBubble> {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel privateChatPanel;
   public PrivateChatMessageRowPool(PrivateChatPanel privateChatPanel) {
      super();
      this.privateChatPanel = privateChatPanel;
   }

   @Override
   public PrivateChatPanel.MessageBubble newObject() {
      return (this.privateChatPanel).new MessageBubble(this.privateChatPanel.renderContext);
   }
}
