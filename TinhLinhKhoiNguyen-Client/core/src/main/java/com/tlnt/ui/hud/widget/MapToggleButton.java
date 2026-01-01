package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class MapToggleButton extends ShapeButton {
   public TextureAsset normalTexture;

   public MapToggleButton(RenderContext renderContext) {
      super(renderContext);
      this.normalTexture = TextureAsset.obtain("ui/209");
      this.setSize(77.0F, 76.0F);
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
         this.setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH - 595),
            (float)(WorldConfig.VIEWPORT_HEIGHT - 9),
            18
         );
      } else {
         this.setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH - 650),
            (float)(WorldConfig.VIEWPORT_HEIGHT - 24),
            18
         );
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTexture(this.normalTexture, this.getX(), this.getY(), Anchor.BOTTOM_LEFT);
   }

}
