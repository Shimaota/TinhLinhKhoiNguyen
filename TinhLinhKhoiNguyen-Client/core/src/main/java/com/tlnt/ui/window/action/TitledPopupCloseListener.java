package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class TitledPopupCloseListener extends ClickListener {
   public com.tlnt.ui.window.action.TitledPopup titledPopup;
   public com.tlnt.ui.window.action.TitledPopup.CloseButton closeButton;
   public TitledPopupCloseListener(TitledPopup.CloseButton closeButton, TitledPopup popup) {
      super();
      this.closeButton = closeButton;
      this.titledPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.closeButton.titledPopup.hide();
   }
}
