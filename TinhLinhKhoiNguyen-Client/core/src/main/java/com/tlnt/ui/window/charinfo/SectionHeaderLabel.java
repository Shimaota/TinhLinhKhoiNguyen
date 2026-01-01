package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class SectionHeaderLabel extends BaseTable {
   public String text;

   public SectionHeaderLabel(String text, RenderContext renderContext) {
      super(renderContext);
      this.text = text;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            this.text,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            30.0F,
            "#8B4E23"
         );
   }

}
