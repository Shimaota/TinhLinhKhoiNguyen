package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class TabbedWindowCloseClickListener extends ClickListener {
   public com.tlnt.ui.window.action.TabbedWindow tabbedWindow;
   public com.tlnt.ui.window.action.TabbedWindow.CloseButton closeButton;
   public TabbedWindowCloseClickListener(TabbedWindow.CloseButton closeButton, TabbedWindow window) {
      super();
      this.closeButton = closeButton;
      this.tabbedWindow = window;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.closeButton.tabbedWindow.hide();
   }
}
