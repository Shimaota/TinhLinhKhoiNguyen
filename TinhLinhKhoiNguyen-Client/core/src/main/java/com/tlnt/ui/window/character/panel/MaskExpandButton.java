package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ShapeButton;

public final class MaskExpandButton extends ShapeButton {
   public com.tlnt.ui.window.character.panel.MaskPanel.MaskSkillPanel maskSkillPanel;
   public CompositeSprite buttonSprite;

   public MaskExpandButton(MaskPanel.MaskSkillPanel panel, RenderContext renderContext) {
      super(renderContext);
      this.maskSkillPanel = panel;
      String leftPath = "ui/10";
      String midPath = "ui/11";
      String rightPath = "ui/12";
      this.buttonSprite = CompositeSprite.obtain(leftPath, midPath, rightPath, false);
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
            "Mở rộng",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            24.0F,
            Color.WHITE
         );
      super.renderContext.resetBatchColor();
   }

}
