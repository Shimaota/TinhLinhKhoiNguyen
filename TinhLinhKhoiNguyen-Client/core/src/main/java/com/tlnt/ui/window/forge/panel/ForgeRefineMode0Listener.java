package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ForgeRefineMode0Listener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeRefinePanel.ContentPanel contentPanel;
   public ForgeRefineMode0Listener(ForgeRefinePanel.ContentPanel panel) {
      super();
      this.contentPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendPvpCancel(0);
   }
}
