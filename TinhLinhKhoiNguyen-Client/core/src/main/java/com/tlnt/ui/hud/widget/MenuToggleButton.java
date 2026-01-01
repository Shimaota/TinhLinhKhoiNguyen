package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class MenuToggleButton extends ShapeButton {
   public TextureAsset texture;

   public MenuToggleButton(RenderContext renderContext) {
      super(renderContext);
      this.texture = TextureAsset.obtain("ui/206");
      this.setSize(67.0F, 63.0F);
      boolean isIOS = Gdx.app.getType() == Application.ApplicationType.iOS;
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (isIOS ? 70 : 15));
      float posY = (float)(WorldConfig.VIEWPORT_HEIGHT - (isIOS ? 30 : 15));
      this.setPosition(posX, posY, 18);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext.drawTextureSized(this.texture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

}
