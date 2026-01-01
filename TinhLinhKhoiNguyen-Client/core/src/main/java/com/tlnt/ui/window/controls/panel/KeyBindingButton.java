package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.TextButton;

public final class KeyBindingButton extends TextButton {
   public com.tlnt.ui.window.controls.panel.KeyBindingPanel.KeyBindingRow keyBindingRow;
   public TextureAsset buttonSprite;

   public KeyBindingButton(KeyBindingPanel.KeyBindingRow row, String text, RenderContext ctx) {
      super(text, ctx);
      this.keyBindingRow = row;
      this.buttonSprite = TextureAsset.obtain("ui/137");
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTextureSized(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

   @Override
   public final void drawLabel() {
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            Input.Keys.toString(Settings.keybindings[this.keyBindingRow.keyBindIndex]),
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
   }

}
