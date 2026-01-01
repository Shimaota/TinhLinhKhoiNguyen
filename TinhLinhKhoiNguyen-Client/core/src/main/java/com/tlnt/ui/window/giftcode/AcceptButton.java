package com.tlnt.ui.window.giftcode;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class AcceptButton extends TextButton {
   public com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog dialog;
   public CompositeSprite buttonSprite;

   public AcceptButton(GiftcodeEventConfirmDialog dialog, String text, RenderContext ctx) {
      super(text, ctx);
      this.dialog = dialog;
      String leftPath = "ui/54";
      String middlePath = "ui/55";
      String rightPath = "ui/56";
      this.buttonSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
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
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            24.0F,
            Color.WHITE
         );
   }

}
