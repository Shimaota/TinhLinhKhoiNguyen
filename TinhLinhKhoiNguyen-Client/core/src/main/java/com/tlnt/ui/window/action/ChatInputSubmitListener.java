package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ChatInputSubmitListener extends ClickListener {
   public com.tlnt.ui.window.action.ChatInputPopup.InputBar chatInputBar;
   public ChatInputSubmitListener(ChatInputPopup.InputBar inputBar) {
      super();
      this.chatInputBar = inputBar;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.chatInputBar.sendChatMessage();
   }
}
