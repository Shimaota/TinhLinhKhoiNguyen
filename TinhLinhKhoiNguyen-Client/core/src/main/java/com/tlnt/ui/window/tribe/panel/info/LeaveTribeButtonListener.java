package com.tlnt.ui.window.tribe.panel.info;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class LeaveTribeButtonListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel panel;
   public LeaveTribeButtonListener(TribeInfoPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestMarket();
   }
}
