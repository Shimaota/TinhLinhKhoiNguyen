package com.tlnt.ui.window.giftcode;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class GiftcodeConfirmAcceptListener extends ClickListener {
   public com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog dialog;
   public GiftcodeConfirmAcceptListener(GiftcodeEventConfirmDialog dialog) {
      super();
      this.dialog = dialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Runnable confirmAction = this.dialog.confirmAction;
      this.dialog.confirmAction = null;
      if (confirmAction != null) {
         confirmAction.run();
      } else {
         ServerPacketSender.getInstance().sendEmote(this.dialog.eventType);
      }
      this.dialog.hide();
   }
}
