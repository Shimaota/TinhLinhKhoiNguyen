package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.ui.hud.WindowManager;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.WindowType;

public final class ChatPopupClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.MessageNotificationButton messageNotificationButton;
   public ChatPopupClickListener(MessageNotificationButton messageNotificationButton) {
      super();
      this.messageNotificationButton = messageNotificationButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      WindowType popupType = WindowType.CHAT_POPUP;
      WindowType windowType = popupType;
      WindowManager windowManager = GameMain.instance.getLoginScreen().debugScrollPane.skeleton;
      Dialog toShow = null;
      Array.ArrayIterator it = windowManager.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof Dialog) {
            Dialog dialog;
            if ((dialog = (Dialog)child).windowType == windowType && !dialog.isVisible()) {
               toShow = dialog;
            } else {
               dialog.hide();
            }
         }
      }

      if (toShow != null) {
         toShow.show();
      }
   }
}
