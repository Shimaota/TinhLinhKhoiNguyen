package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class ChatKeyboardInputListener extends InputListener {
   public com.tlnt.ui.window.action.ChatInputPopup.InputBar inputBar;
   public ChatKeyboardInputListener(ChatInputPopup.InputBar inputBar) {
      super();
      this.inputBar = inputBar;
   }

   @Override
   public final boolean keyUp(InputEvent event, int keycode) {
      if (keycode == 66) {
         this.inputBar.sendChatMessage();
         return true;
      } else {
         return super.keyUp(event, keycode);
      }
   }
}
