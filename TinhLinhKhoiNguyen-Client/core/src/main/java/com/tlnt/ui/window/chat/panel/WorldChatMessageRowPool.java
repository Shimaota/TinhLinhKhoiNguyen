package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class WorldChatMessageRowPool extends Pool<ChatMessageRow> {
   public com.tlnt.ui.window.chat.panel.WorldChatPanel worldChatPanel;
   public WorldChatMessageRowPool(WorldChatPanel worldChatPanel) {
      super();
      this.worldChatPanel = worldChatPanel;
   }

   @Override
   public ChatMessageRow newObject() {
      return new com.tlnt.ui.window.chat.widget.ChatMessageRow(this.worldChatPanel.renderContext);
   }
}
