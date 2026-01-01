package com.tlnt.ui.window.chest;

import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.chest.panel.ChestInventoryPanel;

public class ChestWindow extends TabbedWindow {
   public ChestInventoryPanel OVERLAY_COLOR;

   public ChestWindow(RenderContext ctx) {
      super(ctx, WindowType.BOX_POPUP);
      this.addTab("Rương đồ", "icon/266", this.OVERLAY_COLOR = new ChestInventoryPanel(super.renderContext));
   }

   @Override
   public final void onOpen() {
      AudioManager.playSound("chest_open", null);
   }

   @Override
   public final void onClose() {
      AudioManager.playSound("chest_close", null);
   }

}
