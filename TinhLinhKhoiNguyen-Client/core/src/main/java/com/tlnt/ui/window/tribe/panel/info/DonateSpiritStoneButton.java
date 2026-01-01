package com.tlnt.ui.window.tribe.panel.info;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class DonateSpiritStoneButton extends TextButton {
   public com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel panel;
   public CompositeSprite buttonSprite;

   public DonateSpiritStoneButton(TribeInfoPanel panel, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.panel = panel;
      String startPath = "ui/10";
      this.buttonSprite = CompositeSprite.obtain(
         startPath, "ui/11", "ui/12", false
      );
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
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
            20.0F,
            Color.WHITE
         );
   }

}
