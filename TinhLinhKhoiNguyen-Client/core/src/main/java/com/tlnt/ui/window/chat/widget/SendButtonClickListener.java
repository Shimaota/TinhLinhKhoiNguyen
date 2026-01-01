package com.tlnt.ui.window.chat.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SendButtonClickListener extends ClickListener {
   public com.tlnt.ui.window.chat.widget.ChatInputBar chatInputBar;
   public SendButtonClickListener(ChatInputBar chatInputBar) {
      super();
      this.chatInputBar = chatInputBar;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.chatInputBar.submitMessage();
   }
}
