package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class MpPotionSlotButtonClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.MpPotionSlotButton mpPotionSlotButton;
   public MpPotionSlotButtonClickListener(MpPotionSlotButton mpPotionSlotButton) {
      super();
      this.mpPotionSlotButton = mpPotionSlotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.mpPotionSlotButton.usePotion();
   }
}
