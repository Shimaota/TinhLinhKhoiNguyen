package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class MountButton extends ShapeButton {
   public TextureAsset mountIcon;

   public MountButton(RenderContext renderContext) {
      super(renderContext);
      this.mountIcon = TextureAsset.obtain("ui/198");
      this.setSize(61.0F, 49.0F);
      float posX = Gdx.app.getType() != Application.ApplicationType.iOS ? 50.0F : 145.0F;
      this.setPosition(posX, (float)(WorldConfig.VIEWPORT_HEIGHT - 548), 10);
      this.addListener(new MountToggleClickListener(this));
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTexture(this.mountIcon, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
      super.renderContext.resetBatchColor();
   }

}
