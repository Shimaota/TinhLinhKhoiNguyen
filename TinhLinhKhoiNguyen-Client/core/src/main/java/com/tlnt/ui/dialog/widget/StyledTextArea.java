package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextArea;

public final class StyledTextArea extends TextArea {
   public NinePatchAsset backgroundPatch2;

   public StyledTextArea(String text, RenderContext renderContext) {
      super(text, renderContext, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
      String patchPath = "ui/21";
      this.backgroundPatch2 = NinePatchAsset.obtain(patchPath, 20);
      this.setSize(606.0F, 53.0F);
      this.setPosition(17.0F, 15.0F);
   }

   @Override
   public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
      super.renderContext.setBatchColorRgba(1.0F, 1.0F, 1.0F, 0.5F);
      super.renderContext.resetBatchColor();
   }

   @Override
   public final void drawText(Batch batch, BitmapFont font, float x, float y) {
      super.drawText(batch, font, x, y - 5.0F);
   }

}
