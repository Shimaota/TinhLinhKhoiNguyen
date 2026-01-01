package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class NearbyChatMessagePool extends Pool<ChatMessageRow> {
   public com.tlnt.ui.window.chat.panel.NearbyChatPanel nearbyChatPanel;
   public NearbyChatMessagePool(NearbyChatPanel nearbyChatPanel) {
      super();
      this.nearbyChatPanel = nearbyChatPanel;
   }

   @Override
   public ChatMessageRow newObject() {
      return new com.tlnt.ui.window.chat.widget.ChatMessageRow(this.nearbyChatPanel.renderContext);
   }
}
