package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class GiftcodeRedeemConfirmListener extends ClickListener {
   public com.tlnt.ui.window.giftcode.panel.GiftcodeRedeemPanel panel;
   public GiftcodeRedeemConfirmListener(GiftcodeRedeemPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.panel.redeemGiftcode();
   }
}
