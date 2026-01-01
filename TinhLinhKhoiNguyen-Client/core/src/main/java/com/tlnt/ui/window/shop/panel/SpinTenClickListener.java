package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class SpinTenClickListener extends ClickListener {
   public com.tlnt.ui.window.shop.panel.LuckyWheelPanel luckyWheelPanel;
   public SpinTenClickListener(LuckyWheelPanel panel) {
      super();
      this.luckyWheelPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendNotificationAction(1);
   }
}
