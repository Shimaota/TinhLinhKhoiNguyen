package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.DeformableSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.RenderListener;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PlayerPortrait extends ShapeButton implements RenderListener {
   public TextureAsset frameTexture;
   public DeformableSprite hpArcSprite;
   public TextureAsset avatarFrameTexture;
   public TextureAsset hpBarBg;
   public TextureAsset hpBarFill;
   public TextureAsset mpBarFill;
   public TextureAsset expBarFill;
   public PlayerEntity playerEntity;

   public PlayerPortrait(RenderContext renderContext) {
      super(renderContext);
      this.frameTexture = TextureAsset.obtain("ui/3");
      this.hpArcSprite = new DeformableSprite(TextureAsset.obtain("ui/2").getRegion());
      this.avatarFrameTexture = TextureAsset.obtain("ui/1");
      this.hpBarBg = TextureAsset.obtain("ui/4");
      this.hpBarFill = TextureAsset.obtain("ui/5");
      this.mpBarFill = TextureAsset.obtain("ui/6");
      this.expBarFill = TextureAsset.obtain("ui/7");
      this.setSize(430.0F, 168.0F);
      float posY = (float)(WorldConfig.VIEWPORT_HEIGHT - (Gdx.app.getType() != Application.ApplicationType.iOS ? 10 : 30));
      this.setPosition((float)WorldConfig.VIEWPORT_WIDTH / 2.0F, posY, 2);
      this.addListener(new PlayerPortraitClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.playerEntity != null && this.playerEntity.POOL2 != null) {
         float expAngle = 360.0F * ((float)this.playerEntity.POOL2.currentExp / (float)this.playerEntity.POOL2.maxExp);
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawTexture(this.frameTexture, this.getX(), this.getY() + 33.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawRadialSprite(this.hpArcSprite, this.getX() + 11.0F, this.getY() + 43.0F, 115.0F, 115.0F, Anchor.BOTTOM_LEFT, expAngle);
         super.renderContext.drawTexture(this.avatarFrameTexture, this.getX() + 15.0F, this.getY() + 47.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTexture(TextureAsset.obtain(this.playerEntity.POOL2.avatarTexture), this.getX() + 15.0F, this.getY() + 47.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTexture(this.hpBarBg, this.getX() + 3.0F, this.getY(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(FontManager.PLAIN_FONT, this.playerEntity.POOL2.level + "", this.getX() + 68.0F, this.getY() + 25.0F, Anchor.CENTER, 18.0F, Color.WHITE);
         RenderContext ctx = super.renderContext;
         TextureAsset fillTexture = this.hpBarFill;
         float barX = this.getX() + 141.0F;
         float barY = this.getY() + 108.0F;
         ctx.drawTextureCropped(
            fillTexture,
            barX,
            barY,
            Anchor.BOTTOM_LEFT,
            0,
            0,
            (int)((float)this.hpBarFill.getWidth() * this.playerEntity.POOL2.getHpRatio()),
            this.hpBarFill.getHeight()
         );
         ctx = super.renderContext;
         fillTexture = this.mpBarFill;
         barX = this.getX() + 141.0F;
         barY = this.getY() + 71.0F;
         ctx.drawTextureCropped(
            fillTexture,
            barX,
            barY,
            Anchor.BOTTOM_LEFT,
            0,
            0,
            (int)((float)this.mpBarFill.getWidth() * this.playerEntity.POOL2.getMpRatio()),
            this.mpBarFill.getHeight()
         );
         ctx = super.renderContext;
         fillTexture = this.expBarFill;
         barX = this.getX() + 141.0F;
         barY = this.getY() + 52.0F;
         ctx.drawTextureCropped(
            fillTexture,
            barX,
            barY,
            Anchor.BOTTOM_LEFT,
            0,
            0,
            (int)((float)this.expBarFill.getWidth() * this.playerEntity.POOL2.getActivityPointsRatio()),
            this.expBarFill.getHeight()
         );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.playerEntity.getCharacterName(),
               this.getX() + 129.0F,
               this.getY() + 145.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.playerEntity.POOL2.currentHp + "/" + this.playerEntity.POOL2.maxHp,
               this.getX() + 270.0F,
               this.getY() + 114.0F,
               Anchor.BOTTOM_CENTER,
               18.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.playerEntity.POOL2.currentMp + "/" + this.playerEntity.POOL2.maxMp,
               this.getX() + 270.0F,
               this.getY() + 77.0F,
               Anchor.BOTTOM_CENTER,
               18.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }
   }

   @Override
   public final void onRender(float delta) {
      this.setVisible(
         (((
                  this.playerEntity = GameState.targetEntity != null && GameState.targetEntity.getEntityType() == EntityType.PLAYER
                     ? (PlayerEntity)GameState.targetEntity
                     : null
               )
               != null
            ? 1
            : 0) != 0)
      );
   }

}
