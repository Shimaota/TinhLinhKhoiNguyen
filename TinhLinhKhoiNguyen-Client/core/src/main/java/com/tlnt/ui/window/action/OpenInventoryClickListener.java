package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class OpenInventoryClickListener extends ClickListener {
   public com.tlnt.ui.window.action.ChatInputPopup chatInputPopup;
   public OpenInventoryClickListener(ChatInputPopup popup) {
      super();
      this.chatInputPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.chatInputPopup.hide();
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.MESSAGE_POPUP);
   }
}
