package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class PickupButton extends ShapeButton {

   public PickupButton(RenderContext renderContext) {
      super(renderContext);
      this.setSize(241.0F, 241.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (Gdx.app.getType() != Application.ApplicationType.iOS ? 105 : 215));
      this.setPosition(posX, 90.0F, 20);
      this.addListener(new PickupButtonClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext
            .drawTexture(
               TextureAsset.obtain("ui/37"),
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER
            );
      } else {
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/38"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
      }
   }

}
