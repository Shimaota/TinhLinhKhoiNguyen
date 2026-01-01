package com.tlnt.ui.window.chat.panel;

public final class TribeChatScrollTask implements Runnable {
   public com.tlnt.ui.window.chat.panel.TribeChatPanel tribeChatPanel;
   public TribeChatScrollTask(TribeChatPanel tribeChatPanel) {
      super();
      this.tribeChatPanel = tribeChatPanel;
   }

   @Override
   public final void run() {
      this.tribeChatPanel.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
