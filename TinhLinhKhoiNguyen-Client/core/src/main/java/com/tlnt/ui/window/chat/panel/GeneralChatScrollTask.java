package com.tlnt.ui.window.chat.panel;

public final class GeneralChatScrollTask implements Runnable {
   public com.tlnt.ui.window.chat.panel.GeneralChatPanel generalChatPanel;
   public GeneralChatScrollTask(GeneralChatPanel generalChatPanel) {
      super();
      this.generalChatPanel = generalChatPanel;
   }

   @Override
   public final void run() {
      this.generalChatPanel.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
