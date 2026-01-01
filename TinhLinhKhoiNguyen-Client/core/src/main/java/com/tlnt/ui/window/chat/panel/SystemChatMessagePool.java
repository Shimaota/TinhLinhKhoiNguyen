package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class SystemChatMessagePool extends Pool<ChatMessageRow> {
   public com.tlnt.ui.window.chat.panel.SystemChatPanel systemChatPanel;
   public SystemChatMessagePool(SystemChatPanel systemChatPanel) {
      super();
      this.systemChatPanel = systemChatPanel;
   }

   @Override
   public ChatMessageRow newObject() {
      return new com.tlnt.ui.window.chat.widget.ChatMessageRow(this.systemChatPanel.renderContext);
   }
}
