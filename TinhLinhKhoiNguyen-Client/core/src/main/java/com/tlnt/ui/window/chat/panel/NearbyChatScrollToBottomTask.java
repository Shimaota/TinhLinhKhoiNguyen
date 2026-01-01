package com.tlnt.ui.window.chat.panel;

public final class NearbyChatScrollToBottomTask implements Runnable {
   public com.tlnt.ui.window.chat.panel.NearbyChatPanel nearbyChatPanel;
   public NearbyChatScrollToBottomTask(NearbyChatPanel nearbyChatPanel) {
      super();
      this.nearbyChatPanel = nearbyChatPanel;
   }

   @Override
   public final void run() {
      this.nearbyChatPanel.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
