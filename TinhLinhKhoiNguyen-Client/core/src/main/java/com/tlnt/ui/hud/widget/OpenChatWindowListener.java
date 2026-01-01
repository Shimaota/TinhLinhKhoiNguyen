package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class OpenChatWindowListener extends ClickListener {
   public com.tlnt.ui.hud.widget.MessageTicker messageTicker;
   public OpenChatWindowListener(MessageTicker messageTicker) {
      super();
      this.messageTicker = messageTicker;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.MESSAGE_POPUP);
   }
}
