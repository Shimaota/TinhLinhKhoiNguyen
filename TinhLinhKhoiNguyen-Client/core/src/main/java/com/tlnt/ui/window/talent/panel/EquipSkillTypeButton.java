package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class EquipSkillTypeButton extends TextButton {
   public com.tlnt.ui.window.talent.panel.TalentPanel.SkillDetailPanel buttonSprite;

   public EquipSkillTypeButton(TalentPanel.SkillDetailPanel detailPanel, String text, RenderContext ctx) {
      super(text, ctx);
      this.buttonSprite = detailPanel;
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      String leftPath = "ui/10";
      super.renderContext
         .drawCompositeSprite(
            CompositeSprite.obtain(leftPath, "ui/11", "ui/12", false),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
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
