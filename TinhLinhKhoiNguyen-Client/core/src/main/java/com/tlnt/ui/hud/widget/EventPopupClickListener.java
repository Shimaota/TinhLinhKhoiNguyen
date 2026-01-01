package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class EventPopupClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.MenuButtonPanel menuButtonPanel;
   public EventPopupClickListener(MenuButtonPanel menuButtonPanel) {
      super();
      this.menuButtonPanel = menuButtonPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.EVENT_POPUP);
   }
}
