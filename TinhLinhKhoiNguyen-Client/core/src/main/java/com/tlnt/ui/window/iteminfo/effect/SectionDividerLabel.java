package com.tlnt.ui.window.iteminfo.effect;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.ui.widget.BaseTable;

public final class SectionDividerLabel extends BaseTable {
   public String headerText;
   public GlyphLayout textLayout;

   public SectionDividerLabel(RenderContext renderContext, String text) {
      super(renderContext);
      this.headerText = text;
      this.textLayout = new GlyphLayout(FontFactory.scaleFont(FontManager.PLAIN_FONT, 24.0F), this.headerText);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      float centerY = this.getY() + this.getHeight() / 2.0F;
      parentAlpha = this.getX() + this.getWidth() / 2.0F;
      float textRight = this.textLayout.width;
      float textLeft = parentAlpha - textRight / 2.0F;
      textRight = parentAlpha + textRight / 2.0F;
      ShapeDrawer shapeDrawer = super.renderContext.getShapeDrawer();
      shapeDrawer.setColor(RenderContext.parseColor("#8B4E23"));
      super.renderContext.getShapeDrawer().drawLine(this.getX(), centerY, textLeft - 10.0F, centerY, 2.0F);
      super.renderContext.getShapeDrawer().drawLine(textRight + 10.0F, centerY, this.getX() + this.getWidth(), centerY, 2.0F);
      super.renderContext
         .drawTextColorName(FontManager.PLAIN_FONT, this.headerText, parentAlpha, centerY, Anchor.CENTER, 24.0F, "#8B4E23");
   }

}
