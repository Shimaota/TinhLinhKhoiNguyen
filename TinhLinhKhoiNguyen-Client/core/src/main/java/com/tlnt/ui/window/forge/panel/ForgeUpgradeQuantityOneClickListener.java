package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ForgeUpgradeQuantityOneClickListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeUpgradePanel.ContentPanel contentPanel;
   public ForgeUpgradeQuantityOneClickListener(ForgeUpgradePanel.ContentPanel panel) {
      super();
      this.contentPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendPvpAccept(1);
   }
}
