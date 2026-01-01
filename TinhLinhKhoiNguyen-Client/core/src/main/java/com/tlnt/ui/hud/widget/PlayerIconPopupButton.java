package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PlayerIconPopupButton extends ShapeButton {
   public NinePatchAsset background;

   public PlayerIconPopupButton(RenderContext renderContext, PopupMenu popupMenu) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/13", 20);
      this.setSize(70.0F, 70.0F);
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
         this.setPosition(455.0F, (float)(WorldConfig.VIEWPORT_HEIGHT - 8), 10);
      } else {
         this.setPosition(480.0F, (float)(WorldConfig.VIEWPORT_HEIGHT - 30), 10);
      }
      this.addListener(new PopupMenuCloseClickListener(this, popupMenu));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain(PlayerEntity.getInstance().POOL2.factionStatus.faction.displayName),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
   }

}
