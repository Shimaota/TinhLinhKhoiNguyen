package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ExitGameClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.ExitButton exitButton;
   public ExitGameClickListener(ExitButton exitButton) {
      super();
      this.exitButton = exitButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestShop();
   }
}
