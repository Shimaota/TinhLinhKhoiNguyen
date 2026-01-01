package com.tlnt.ui.window.iteminfo;

import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class ShowItemInfoRunnable implements Runnable {
   public com.tlnt.ui.window.iteminfo.DelayedShowPopupRunnable delayRunnable;
   public ShowItemInfoRunnable(DelayedShowPopupRunnable delayRunnable) {
      super();
      this.delayRunnable = delayRunnable;
   }

   @Override
   public final void run() {
      if (this.delayRunnable.popup.pendingConfirmation) {
         GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.INFO_ITEM_POPUP);
         this.delayRunnable.popup.pendingConfirmation = false;
      }
   }
}
