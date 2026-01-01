package com.tlnt.ui.window.map.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.net.task.ServerPacketSender;

public final class MapMarkerClickListener extends ClickListener {
   public com.tlnt.ui.window.map.panel.MapRegionPanel.MapMarkerButton markerButton;
   public com.tlnt.ui.window.map.panel.MapRegionPanel regionPanel;
   public MapMarkerClickListener(MapRegionPanel.MapMarkerButton button, MapRegionPanel panel) {
      super();
      this.markerButton = button;
      this.regionPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (GameMain.instance.getLoginScreen().editorViewport.getId() != this.markerButton.mapMarker.mapId) {
         ServerPacketSender.getInstance().sendSellItem(this.markerButton.outerPanel.mapData.id, this.markerButton.mapMarker.mapId);
      }
   }
}
