package com.tlnt.world.label;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class PlayerHealthBar extends FloatingOverlay {
   /** Health bar colors (the original called Color.valueOf every frame = string parsing + allocation). */
   private static final Color HP_COLOR = Color.valueOf("E31300");
   public float playerEntity = 135.0F;
   public float barHeight = 17.0F;
   public PlayerEntity playerEntity2;

   public PlayerHealthBar(PlayerEntity player) {
      super(new Vector2(0.0F, 20.0F));
      this.playerEntity2 = player;
   }

   @Override
   public final void refresh() {
      this.setVisible(this.playerEntity2.POOL2.showHealthBar);
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      float hpPercent = Math.max(0.0F, Math.min(1.0F, this.playerEntity2.getHpPercent()));
      ShapeDrawer shapeDrawer = ctx.getShapeDrawer();
      float missingPercent = 1.0F - hpPercent;
      shapeDrawer.drawFilledRectangle(x - this.playerEntity / 2.0F, y - this.barHeight / 2.0F, this.playerEntity, this.barHeight, Color.BLACK);
      hpPercent = this.playerEntity * hpPercent;
      float barX = x - this.playerEntity / 2.0F;
      float barY = y - this.barHeight / 2.0F;
      shapeDrawer.drawFilledRectangle(barX, barY, hpPercent, this.barHeight, HP_COLOR);
      shapeDrawer.drawRectangle(x - this.playerEntity / 2.0F, y - this.barHeight / 2.0F, this.playerEntity, this.barHeight, Color.BLACK, 2.0F);
   }

}
