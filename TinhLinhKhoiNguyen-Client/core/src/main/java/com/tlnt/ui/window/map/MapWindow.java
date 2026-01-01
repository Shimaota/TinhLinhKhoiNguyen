package com.tlnt.ui.window.map;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.worldmap.WorldMapPage;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.map.panel.MapRegionPanel;

public final class MapWindow extends TabbedWindow {

   public MapWindow(RenderContext ctx) {
      super(ctx, WindowType.MINI_MAP_POPUP);
   }

   @Override
   public final void onOpen() {
      super.onOpen();
      if (this.getTabCount() == 0) {
         for (WorldMapPage page : WorldMapPage.getPages()) {
            String tabTitle = "Bản đồ";
            this.addTab(tabTitle, "icon/0", new MapRegionPanel(super.renderContext, page));
         }
      }
   }

}
