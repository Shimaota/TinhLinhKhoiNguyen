package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.TextButton;

public class SettingsToggleButton extends TextButton {
   public TextureAsset buttonSprite;
   public TextureAsset checkIconTexture;
   public Color labelColor;

   public SettingsToggleButton(String text, RenderContext renderContext) {
      super(text, renderContext);
      this.buttonSprite = TextureAsset.obtain("ui/63");
      this.checkIconTexture = TextureAsset.obtain("ui/64");
      this.labelColor = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTextureSized(this.buttonSprite, this.getX(), this.getY(), this.getHeight(), this.getHeight(), Anchor.BOTTOM_LEFT);
      if (this.isChecked()) {
         super.renderContext
            .drawTextureSized(
               this.checkIconTexture,
               this.getX() + this.getHeight() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               this.getHeight() - 16.0F,
               this.getHeight() - 16.0F,
               Anchor.CENTER
            );
      }

      super.renderContext.resetBatchColor();
   }

   @Override
   public final void drawLabel() {
      super.renderContext
         .drawText(
            FontManager.PLAIN_FONT,
            super.text,
            this.getX() + this.getHeight() + 8.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.MIDDLE_LEFT,
            24.0F,
            this.labelColor
         );
   }

}
