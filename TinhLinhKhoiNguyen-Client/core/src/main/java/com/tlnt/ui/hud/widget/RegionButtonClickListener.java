package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class RegionButtonClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.RegionButton regionButton;
   public RegionButtonClickListener(RegionButton regionButton) {
      super();
      this.regionButton = regionButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestInventory();
   }
}
