package com.tlnt.ui.window.iteminfo;

import com.badlogic.gdx.Gdx;

public final class DelayedShowPopupRunnable implements Runnable {
   public final com.tlnt.ui.window.iteminfo.ItemInfoPopup popup;
   public DelayedShowPopupRunnable(ItemInfoPopup popup) {
      this.popup = popup;
   }

   @Override
   public final void run() {
      Gdx.app.postRunnable(new ShowItemInfoRunnable(this));
   }
}
