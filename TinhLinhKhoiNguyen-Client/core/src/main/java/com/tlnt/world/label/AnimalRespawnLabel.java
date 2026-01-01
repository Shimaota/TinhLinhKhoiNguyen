package com.tlnt.world.label;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.util.GameUtils;
import com.tlnt.util.TimeFormatter;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class AnimalRespawnLabel extends FloatingOverlay {
   public AnimalEntity playerEntity;

   public AnimalRespawnLabel(AnimalEntity animalEntity) {
      super(new Vector2(0.0F, 52.0F));
      this.playerEntity = animalEntity;
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      String text = "";
      if (this.playerEntity.POOL2.animalType != null) {
         if (GameUtils.hasElapsed(this.playerEntity.POOL2.growStartTime, this.playerEntity.POOL2.animalType.growDurationMs)) {
            text = this.playerEntity.POOL2.animalType.matureName;
         } else {
            long now = System.currentTimeMillis();
            long readyTime;
            long remainingMs;
            if ((remainingMs = (readyTime = this.playerEntity.POOL2.growStartTime + (long)this.playerEntity.POOL2.animalType.growDurationMs) - now) < 0L) {
               remainingMs = 0L;
            }

            text = this.playerEntity.POOL2.animalType.growingName + " (" + TimeFormatter.formatDuration(remainingMs) + ")";
         }
      }

      ctx.drawText(FontManager.DEFAULT_FONT, text, x, y - super.size.y / 2.0F, Anchor.BOTTOM_CENTER, 24.0F, Color.WHITE);
   }

   @Override
   public final void release() {
      super.release();
      this.playerEntity = null;
   }
}
