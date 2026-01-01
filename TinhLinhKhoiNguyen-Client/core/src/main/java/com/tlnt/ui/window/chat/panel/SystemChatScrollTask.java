package com.tlnt.ui.window.chat.panel;

public final class SystemChatScrollTask implements Runnable {
   public com.tlnt.ui.window.chat.panel.SystemChatPanel systemChatPanel;
   public SystemChatScrollTask(SystemChatPanel systemChatPanel) {
      super();
      this.systemChatPanel = systemChatPanel;
   }

   @Override
   public final void run() {
      this.systemChatPanel.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
