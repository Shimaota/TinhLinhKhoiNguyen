package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.world.entity.player.PlayerEntity;

public final class HarvestProgressBar extends BaseComponent {
   public float speed = 1.0F;
   public float active = 0.1F;
   public float autoFlipEnabled = 0.02F;
   public long projectile2;
   public int lastTemplate;
   public boolean projectile;
   public PlayerEntity playerEntity;

   public HarvestProgressBar(PlayerEntity playerEntity) {
      super(playerEntity.getEntity());
      this.playerEntity = playerEntity;
      this.setRenderOrder(1.0E8F);
      this.setEnabled(false);
   }

   public final void startProgress(long startTime, int durationMs) {
      this.projectile2 = startTime;
      this.lastTemplate = durationMs;
      this.projectile = true;
      this.setEnabled(true);
   }

   public final void stopProgress() {
      this.projectile = false;
      this.setEnabled(false);
   }

   @Override
   public final void render(RenderContext ctx) {
      float progress;
      if ((progress = (float)(System.currentTimeMillis() - this.projectile2) / (float)this.lastTemplate) >= 1.0F) {
         this.setEnabled(false);
         this.projectile = false;
      } else {
         if (progress < 0.0F) {
            progress = 0.0F;
         }

         ShapeDrawer drawer;
         ShapeDrawer shapeDrawer = drawer = ctx.getShapeDrawer();
         float barX = this.playerEntity.getPosition().x - this.speed / 2.0F;
         float barY = this.playerEntity.getPosition().y - 0.3F;
         shapeDrawer.drawFilledRectangle(barX, barY, this.speed, this.active, RenderContext.parseColor("#2a0f00"));
         drawer.setColor(Color.WHITE);
         drawer.drawRectangle(this.playerEntity.getPosition().x - this.speed / 2.0F, this.playerEntity.getPosition().y - 0.3F, this.speed, this.active, 0.01F);
         drawer.drawFilledRectangle(
            this.playerEntity.getPosition().x - this.speed / 2.0F + this.autoFlipEnabled,
            this.playerEntity.getPosition().y + this.autoFlipEnabled - 0.3F,
            (this.speed - this.autoFlipEnabled * 2.0F) * progress,
            this.active - this.autoFlipEnabled * 2.0F,
            Color.GREEN
         );
         Vector3 hudPos = new Vector3(this.playerEntity.getPosition(), 0.0F);
         ctx.worldToHud(hudPos);
         ctx.useHudCamera();
         ctx.drawText(FontManager.DEFAULT_FONT, "Đang thu hoạch...", hudPos.x, hudPos.y, Anchor.CENTER, 22.0F, Color.WHITE);
         ctx.usePreviousCamera();
      }
   }

}
