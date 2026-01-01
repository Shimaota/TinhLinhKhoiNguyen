package com.tlnt.world.label;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

/**
 * "Grow fast" button on the selected animal while it is still growing (port of decompiled C3480): frame ui/220 117 × 40, brown text. Click ->
 * C→S op 52 sub 1 int animal id; the server asks for confirmation since it costs sapphires.
 */
public final class AnimalGrowFastButton extends FloatingOverlay {
   private static final float HALF_W = 58.5F;
   private static final float HALF_H = 20.0F;
   private final AnimalEntity animal;
   private final Vector3 touch = new Vector3();
   private boolean pressed;

   public AnimalGrowFastButton(AnimalEntity animal) {
      super(new Vector2(0.0F, 45.0F));
      this.animal = animal;
   }

   @Override
   public final void refresh() {
      boolean growing = this.animal.POOL2.animalType != null
         && this.animal.POOL2.growStartTime + (long)this.animal.POOL2.animalType.growDurationMs > System.currentTimeMillis();
      this.setVisible(growing && GameState.targetEntity == this.animal);
      if (!this.isVisible()) {
         this.pressed = false;
      }
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      if (Gdx.input.isTouched(0)) {
         this.touch.set(Gdx.input.getX(), Gdx.input.getY(), 0.0F);
         ctx.hudViewport.unproject(this.touch);
         this.pressed = Math.abs(this.touch.x - x) <= HALF_W && Math.abs(this.touch.y - y) <= HALF_H;
      } else {
         this.pressed = false;
      }

      if (Gdx.input.justTouched() && this.pressed) {
         ServerPacketSender.getInstance().sendAnimalGrowFast(this.animal.getId());
      }

      if (this.pressed) {
         ctx.setBatchColor(Color.GRAY);
      }

      ctx.drawTextureSized(TextureAsset.obtain("ui/220"), x, y, 117.0F, 40.0F, Anchor.CENTER);
      ctx.drawTextColorName(FontManager.PLAIN_FONT, "Lớn nhanh", x, y, Anchor.CENTER, 20.0F, "#8B4E23");
      ctx.resetBatchColor();
   }
}
