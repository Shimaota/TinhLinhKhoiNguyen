package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;

public final class ImageActor extends Actor {
   public TextureAsset texture;
   public Color tintColor = Color.WHITE;
   public boolean stretchToBounds = true;
   public final Vector2 fixedSize = new Vector2(-1.0F, -1.0F);
   public RenderContext renderContext;

   public ImageActor(RenderContext renderContext) {
      this(renderContext, null);
   }

   public ImageActor(RenderContext renderContext, TextureAsset texture) {
      this.renderContext = renderContext;
      this.texture = null;
   }

   public final ImageActor setTexture(TextureAsset texture) {
      this.texture = texture;
      return this;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.texture != null) {
         this.renderContext.setBatchColor(this.tintColor);
         if (this.stretchToBounds) {
            this.renderContext.drawTextureSized(this.texture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         } else {
            this.renderContext
               .drawTextureFit(
                  this.texture, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, this.fixedSize.x, this.fixedSize.y, Anchor.CENTER
               );
         }

         this.renderContext.resetBatchColor();
      }
   }
}
