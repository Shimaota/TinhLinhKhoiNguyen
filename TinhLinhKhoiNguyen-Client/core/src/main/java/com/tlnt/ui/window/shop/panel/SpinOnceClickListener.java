package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class SpinOnceClickListener extends ClickListener {
   public com.tlnt.ui.window.shop.panel.LuckyWheelPanel luckyWheelPanel;
   public SpinOnceClickListener(LuckyWheelPanel panel) {
      super();
      this.luckyWheelPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender sender = ServerPacketSender.getInstance();
      sender.sendNotificationAction(0);
   }
}
