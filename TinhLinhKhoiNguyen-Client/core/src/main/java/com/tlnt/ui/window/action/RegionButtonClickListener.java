package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class RegionButtonClickListener extends ClickListener {
   public com.tlnt.ui.window.action.RegionInfoPopup regionInfoPopup;
   public com.tlnt.ui.window.action.RegionInfoPopup.RegionEntryButton regionButton;
   public RegionButtonClickListener(RegionInfoPopup.RegionEntryButton regionButton, RegionInfoPopup popup) {
      super();
      this.regionButton = regionButton;
      this.regionInfoPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.regionButton.regionData != null) {
         ServerPacketSender.getInstance().sendSplitItem(this.regionButton.regionData.regionId);
      }
   }
}
