package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class ExitButton extends ShapeButton {
   public TextureAsset texture;

   public ExitButton(RenderContext renderContext) {
      super(renderContext);
      this.texture = TextureAsset.obtain("ui/119");
      this.setSize(92.0F, 45.0F);
      float posY = (float)(WorldConfig.VIEWPORT_HEIGHT - 187);
      this.setPosition(10.0F, posY, 10);
      this.addListener(new ExitGameClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext.drawTextureSized(this.texture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String label = "Thoát";
      float textX = this.getX() + this.getWidth() / 2.0F;
      float textY = this.getY() + this.getHeight() / 1.8F;
      ctx.drawTextColorName(font, label, textX, textY, Anchor.CENTER, 22.0F, "#8B4E23");
      super.renderContext.resetBatchColor();
   }

}
