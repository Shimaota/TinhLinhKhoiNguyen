package com.tlnt.ui.window.countdown;

import com.badlogic.gdx.utils.Pool;

public final class CountdownOptionButtonPool extends Pool<CountdownPopup.CountdownOptionButton> {
   public com.tlnt.ui.window.countdown.CountdownPopup popup;
   public CountdownOptionButtonPool(CountdownPopup popup) {
      super();
      this.popup = popup;
   }

   @Override
   public CountdownPopup.CountdownOptionButton newObject() {
      return (this.popup).new CountdownOptionButton("", this.popup.renderContext);
   }
}
