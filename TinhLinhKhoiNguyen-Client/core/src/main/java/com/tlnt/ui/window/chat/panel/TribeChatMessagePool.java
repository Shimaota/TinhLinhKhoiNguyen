package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class TribeChatMessagePool extends Pool<ChatMessageRow> {
   public com.tlnt.ui.window.chat.panel.TribeChatPanel tribeChatPanel;
   public TribeChatMessagePool(TribeChatPanel tribeChatPanel) {
      super();
      this.tribeChatPanel = tribeChatPanel;
   }

   @Override
   public ChatMessageRow newObject() {
      return new com.tlnt.ui.window.chat.widget.ChatMessageRow(this.tribeChatPanel.renderContext);
   }
}
