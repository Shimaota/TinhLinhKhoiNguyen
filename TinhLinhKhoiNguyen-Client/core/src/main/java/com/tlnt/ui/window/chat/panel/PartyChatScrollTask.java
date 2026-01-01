package com.tlnt.ui.window.chat.panel;

public final class PartyChatScrollTask implements Runnable {
   public com.tlnt.ui.window.chat.panel.PartyChatPanel partyChatPanel;
   public PartyChatScrollTask(PartyChatPanel partyChatPanel) {
      super();
      this.partyChatPanel = partyChatPanel;
   }

   @Override
   public final void run() {
      this.partyChatPanel.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
