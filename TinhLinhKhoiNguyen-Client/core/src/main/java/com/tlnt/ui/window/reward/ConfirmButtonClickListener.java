package com.tlnt.ui.window.reward;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ConfirmButtonClickListener extends ClickListener {
   public com.tlnt.ui.window.reward.RewardPopup rewardPopup;
   public ConfirmButtonClickListener(RewardPopup popup) {
      super();
      this.rewardPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.rewardPopup.hide();
   }
}
