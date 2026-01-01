package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class CharacterStatsPanel extends BaseTable {
   public com.tlnt.ui.dialog.CharacterCreateDialog dialog;
   public NinePatchAsset backgroundPatch;

   public CharacterStatsPanel(CharacterCreateDialog dialog, RenderContext renderContext) {
      super(renderContext);
      this.dialog = dialog;
      this.backgroundPatch = NinePatchAsset.obtain("ui/13", 20);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(FontManager.DEFAULT_FONT, "Tấn công", this.getX() + 10.0F, this.getY() + 165.0F, Anchor.BOTTOM_LEFT, 24.0F, Color.WHITE);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Sinh lực",
            this.getX() + 10.0F,
            this.getY() + 117.0F,
            Anchor.BOTTOM_LEFT,
            24.0F,
            Color.WHITE
         );
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Trí lực",
            this.getX() + 10.0F,
            this.getY() + 69.0F,
            Anchor.BOTTOM_LEFT,
            24.0F,
            Color.WHITE
         );
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Phòng thủ",
            this.getX() + 10.0F,
            this.getY() + 21.0F,
            Anchor.BOTTOM_LEFT,
            24.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

}
