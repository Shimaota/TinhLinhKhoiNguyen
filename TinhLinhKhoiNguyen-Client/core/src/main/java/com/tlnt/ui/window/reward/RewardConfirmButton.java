package com.tlnt.ui.window.reward;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class RewardConfirmButton extends TextButton {
   public com.tlnt.ui.window.reward.RewardPopup rewardPopup;
   public CompositeSprite buttonSprite;

   public RewardConfirmButton(RewardPopup popup, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.rewardPopup = popup;
      String leftPath = "ui/10";
      String middlePath = "ui/11";
      String rightPath = "ui/12";
      this.buttonSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
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
            24.0F,
            Color.WHITE
         );
   }

}
