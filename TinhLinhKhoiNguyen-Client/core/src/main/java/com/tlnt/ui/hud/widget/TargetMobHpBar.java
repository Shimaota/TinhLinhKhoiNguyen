package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.mob.MobData;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.RenderListener;
import com.tlnt.world.entity.mob.MobEntity;

public final class TargetMobHpBar extends ShapeButton implements RenderListener {
   public TextureAsset bgTexture;
   public TextureAsset hpBarTexture;
   public MobEntity targetMob;

   public TargetMobHpBar(RenderContext renderContext) {
      super(renderContext);
      this.bgTexture = TextureAsset.obtain("ui/9");
      this.hpBarTexture = TextureAsset.obtain("ui/8");
      this.setSize(356.0F, 20.0F);
      float posX = (float)WorldConfig.VIEWPORT_WIDTH / 2.0F;
      this.setPosition(posX, (float)(WorldConfig.VIEWPORT_HEIGHT - 135), 2);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.targetMob != null) {
         super.renderContext.drawTextureSized(this.bgTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         RenderContext ctx = super.renderContext;
         TextureAsset texture = this.hpBarTexture;
         float barX = this.getX() + 2.0F;
         float barY = this.getY() + 2.0F;
         Anchor anchor = Anchor.BOTTOM_LEFT;
         float barWidth = (float)this.hpBarTexture.getWidth();
         MobData mobData = this.targetMob.POOL2;
         ctx.drawTextureCropped(
            texture,
            barX,
            barY,
            anchor,
            0,
            0,
            (int)(barWidth * ((float)this.targetMob.POOL2.currentHp / (float)mobData.maxHp)),
            this.hpBarTexture.getHeight()
         );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.targetMob.POOL2.currentHp + "/" + this.targetMob.POOL2.maxHp,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F + 2.0F,
               Anchor.CENTER,
               14.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Lv" + this.targetMob.POOL2.level + ". " + this.targetMob.POOL2.template.variants[this.targetMob.POOL2.variantIndex].displayName,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() + 12.0F,
               Anchor.BOTTOM_CENTER,
               20.0F,
               Color.WHITE
            );
      }
   }

   @Override
   public final void onRender(float delta) {
      int hasTarget;
      if ((
            this.targetMob = GameState.targetEntity != null && GameState.targetEntity.getEntityType() == EntityType.MOB
               ? (MobEntity)GameState.targetEntity
               : null
         )
         != null) {
         hasTarget = 1;
      } else {
         hasTarget = 0;
      }

      this.setVisible((hasTarget != 0));
   }

}
