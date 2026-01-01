package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.Settings;

public final class KeyCaptureStartListener extends ClickListener {
   public com.tlnt.ui.window.controls.panel.KeyBindingPanel.KeyBindingRow keyBindingRow;
   public KeyCaptureStartListener(KeyBindingPanel.KeyBindingRow row) {
      super();
      this.keyBindingRow = row;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (!Settings.isCapturingKeybind) {
         Settings.pendingKeybindIndex = this.keyBindingRow.keyBindIndex;
         Settings.isCapturingKeybind = true;
      }
   }
}
