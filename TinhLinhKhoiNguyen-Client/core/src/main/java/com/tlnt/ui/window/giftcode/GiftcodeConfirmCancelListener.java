package com.tlnt.ui.window.giftcode;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class GiftcodeConfirmCancelListener extends ClickListener {
   public com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog dialog;
   public GiftcodeConfirmCancelListener(GiftcodeEventConfirmDialog dialog) {
      super();
      this.dialog = dialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.dialog.confirmAction = null;
      this.dialog.hide();
   }
}
