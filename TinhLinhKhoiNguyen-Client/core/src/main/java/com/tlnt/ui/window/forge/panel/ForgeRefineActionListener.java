package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ForgeRefineActionListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeRefinePanel.ContentPanel contentPanel;
   public ForgeRefineActionListener(ForgeRefinePanel.ContentPanel panel) {
      super();
      this.contentPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender sender = ServerPacketSender.getInstance();
      sender.sendPvpAccept(1);
   }
}
