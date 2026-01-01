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

public final class ChatButton extends ShapeButton {

   public ChatButton(RenderContext renderContext) {
      super(renderContext);
      this.setSize(80.0F, 80.0F);
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
         this.setPosition((float)(WorldConfig.VIEWPORT_WIDTH - 70), 468.0F, 20);
      } else {
         this.setPosition((float)(WorldConfig.VIEWPORT_WIDTH - 180), 468.0F, 20);
      }
      this.addListener(new DuelRequestClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext
         .drawTextureSized(TextureAsset.obtain("ui/36"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

}
