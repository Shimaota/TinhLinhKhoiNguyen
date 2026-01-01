package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ShapeButton;

public final class AcceptQuestButton extends ShapeButton {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.DailyQuestListPanel parentPanel;
   public CompositeSprite buttonSprite;

   public AcceptQuestButton(DailyQuestPanel.DailyQuestListPanel listPanel, RenderContext ctx) {
      super(ctx);
      this.parentPanel = listPanel;
      String leftPath = "ui/10";
      String middlePath = "ui/11";
      String rightPath = "ui/12";
      this.buttonSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Nhận NV mới",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            24.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

   @Override
   public final void drawBackground() {
   }

}
