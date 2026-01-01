package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class DuelRequestClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.ChatButton chatButton;
   public DuelRequestClickListener(ChatButton chatButton) {
      super();
      this.chatButton = chatButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestDuel();
   }
}
