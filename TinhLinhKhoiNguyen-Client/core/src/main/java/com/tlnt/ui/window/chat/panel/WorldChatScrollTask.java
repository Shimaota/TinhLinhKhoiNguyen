package com.tlnt.ui.window.chat.panel;

public final class WorldChatScrollTask implements Runnable {
   public com.tlnt.ui.window.chat.panel.WorldChatPanel worldChatPanel;
   public WorldChatScrollTask(WorldChatPanel worldChatPanel) {
      super();
      this.worldChatPanel = worldChatPanel;
   }

   @Override
   public final void run() {
      this.worldChatPanel.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
