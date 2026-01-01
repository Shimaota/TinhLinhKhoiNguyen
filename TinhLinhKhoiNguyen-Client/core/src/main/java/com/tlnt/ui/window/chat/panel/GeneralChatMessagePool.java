package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class GeneralChatMessagePool extends Pool<ChatMessageRow> {
   public com.tlnt.ui.window.chat.panel.GeneralChatPanel generalChatPanel;
   public GeneralChatMessagePool(GeneralChatPanel generalChatPanel) {
      super();
      this.generalChatPanel = generalChatPanel;
   }

   @Override
   public ChatMessageRow newObject() {
      return new com.tlnt.ui.window.chat.widget.ChatMessageRow(this.generalChatPanel.renderContext);
   }
}
