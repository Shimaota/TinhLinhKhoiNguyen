package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;

public class TextButton extends ShapeButton {
   public String text;
   public float buttonSprite;

   public TextButton(String text, RenderContext renderContext) {
      this(text, 24.0F, renderContext);
   }

   public TextButton(String text, float fontSize, RenderContext renderContext) {
      super(renderContext);
      this.text = text;
      this.buttonSprite = 24.0F;
   }

   public final TextButton setText(String text) {
      this.text = text;
      return this;
   }

   public final String getText() {
      return this.text;
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      this.drawLabel();
   }

   public void drawLabel() {
      Color labelColor = Color.LIGHT_GRAY;
      if (this.isDisabled()) {
         labelColor = Color.DARK_GRAY;
      } else if (this.isPressed()) {
         labelColor = Color.DARK_GRAY;
      } else if (this.isOver()) {
         labelColor = Color.GRAY;
      }

      labelColor = 0.299F * labelColor.r + 0.587F * labelColor.g + 0.114F * labelColor.b < 0.5F ? Color.WHITE : Color.BLACK;
      super.renderContext
         .drawText(
            FontManager.PLAIN_FONT,
            this.text,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            this.buttonSprite,
            labelColor
         );
   }
}
