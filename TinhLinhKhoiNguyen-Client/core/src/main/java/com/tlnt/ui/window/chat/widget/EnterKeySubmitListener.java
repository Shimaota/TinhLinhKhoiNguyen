package com.tlnt.ui.window.chat.widget;

import com.tlnt.ui.widget.TextField;

public final class EnterKeySubmitListener implements TextField.TextFieldListener {
   public ChatInputBar chatInputBar;

   public EnterKeySubmitListener(ChatInputBar chatInputBar) {
      super();
      this.chatInputBar = chatInputBar;
   }

   @Override
   public final void keyTyped(char character) {
      if (character == 10 || character == 13) {
         this.chatInputBar.submitMessage();
      }
   }
}
