package com.tlnt.ui.window.controls;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel;
import com.tlnt.ui.window.controls.panel.KeyBindingPanel;

public class ControlsWindow extends TabbedWindow {

   public ControlsWindow(RenderContext ctx) {
      super(ctx, WindowType.SETTING_2_POPUP);
      this.addTab("Phím tắt", "icon/253", new KeyBindingPanel(super.renderContext));
      this.addTab("Thiết lập", "icon/254", new AutoPlaySettingsPanel(super.renderContext));
   }

}
