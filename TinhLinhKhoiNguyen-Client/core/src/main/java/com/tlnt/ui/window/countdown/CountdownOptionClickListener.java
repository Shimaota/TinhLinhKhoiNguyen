package com.tlnt.ui.window.countdown;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class CountdownOptionClickListener extends ClickListener {
   public com.tlnt.ui.window.countdown.CountdownPopup.CountdownOptionButton optionButton;
   public com.tlnt.ui.window.countdown.CountdownPopup popup;
   public CountdownOptionClickListener(CountdownPopup.CountdownOptionButton optionButton, CountdownPopup popup) {
      super();
      this.optionButton = optionButton;
      this.popup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendDuelAction(this.optionButton.optionIndex);
   }
}
