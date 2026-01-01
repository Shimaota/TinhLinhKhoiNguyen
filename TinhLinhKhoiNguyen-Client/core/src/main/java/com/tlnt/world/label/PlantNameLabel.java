package com.tlnt.world.label;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class PlantNameLabel extends FloatingOverlay {
   public PlantEntity playerEntity;

   public PlantNameLabel(PlantEntity plantEntity) {
      super(new Vector2(0.0F, 52.0F));
      this.playerEntity = plantEntity;
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      ctx.drawText(FontManager.DEFAULT_FONT, this.playerEntity.POOL2.template.name, x, y - super.size.y / 2.0F, Anchor.BOTTOM_CENTER, 24.0F, Color.WHITE);
   }

   @Override
   public final void release() {
      super.release();
      this.playerEntity = null;
   }
}
