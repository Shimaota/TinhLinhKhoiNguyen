package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ShapeButton;

public final class PlayerNameButton extends ShapeButton {
   public CompositeSprite buttonSprite;

   public PlayerNameButton(RenderContext renderContext) {
      super(renderContext);
      String upPath = "ui/10";
      String downPath = "ui/11";
      String checkedPath = "ui/12";
      this.buttonSprite = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
      this.setSize(308.0F, 63.0F);
      boolean isIOS = Gdx.app.getType() == Application.ApplicationType.iOS;
      this.setPosition(
         (float)(WorldConfig.VIEWPORT_WIDTH - (isIOS ? 322 : 267)),
         (float)(WorldConfig.VIEWPORT_HEIGHT - (isIOS ? 30 : 15)),
         18
      );
      this.addListener(new MinimapWindowClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            GameMain.instance.getLoginScreen().editorViewport.POOL2,
            this.getX() + this.getWidth() / 2.05F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            24.0F,
            Color.WHITE
         );
      super.renderContext.resetBatchColor();
   }

}
