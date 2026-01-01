package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class PartyChatMessagePool extends Pool<ChatMessageRow> {
   public com.tlnt.ui.window.chat.panel.PartyChatPanel partyChatPanel;
   public PartyChatMessagePool(PartyChatPanel partyChatPanel) {
      super();
      this.partyChatPanel = partyChatPanel;
   }

   @Override
   public ChatMessageRow newObject() {
      return new com.tlnt.ui.window.chat.widget.ChatMessageRow(this.partyChatPanel.renderContext);
   }
}
