package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.GameTextField;

public class StyledTextField extends GameTextField {
   public NinePatchAsset backgroundPatch;

   public StyledTextField(String text, RenderContext renderContext) {
      super(
         text,
         renderContext,
         FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F),
         FontFactory.scaleFontWithColor(FontManager.TEXT_FIELD_MESSAGE_FONT, 22.0F, Color.RED)
      );
      this.backgroundPatch = NinePatchAsset.obtain("ui/21", 20);
      this.setSize(606.0F, 53.0F);
      this.setPosition(17.0F, 15.0F);
   }

   @Override
   public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
   }

   @Override
   public void drawText(Batch batch, BitmapFont font, float x, float y) {
      super.drawText(batch, font, x, y - 6.0F);
   }

}
