package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.engine.render.RenderContext;

public final class InputRowTextField extends StyledTextField {
   public com.tlnt.ui.dialog.widget.TextInputRow backgroundPatch;
   public InputRowTextField(TextInputRow inputRow, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.backgroundPatch = inputRow;
   }

   @Override
   public final void drawText(Batch batch, BitmapFont font, float x, float y) {
      super.drawText(batch, font, x, y);
   }
}
