package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.TextButton;

public final class TribeListButton extends TextButton {
   public com.tlnt.ui.window.tribe.panel.TribeListEntry tribeListEntry;
   public TextureAsset buttonSprite;

   public TribeListButton(TribeListEntry tribeListEntry, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.tribeListEntry = tribeListEntry;
      this.buttonSprite = TextureAsset.obtain("ui/102");
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
            super.text,
            this.getX() + this.getWidth() / 2.05F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
   }

}
