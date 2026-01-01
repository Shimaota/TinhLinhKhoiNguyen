package com.tlnt.ui.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.tlnt.core.GameMain;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class FadeOverlay extends BaseTable {
   public float alpha;
   public float elapsedTime;
   public float duration = 1.0F;

   public FadeOverlay(RenderContext renderContext) {
      super(renderContext);
      this.setFillParent(true);
   }

   @Override
   public final void reset() {
      this.elapsedTime = 0.0F;
      this.alpha = 1.0F;
      GameMain.instance.getLoginScreen().active = false;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (GameMain.instance.getLoginScreen().active) {
         this.reset();
      }

      if (!(this.elapsedTime > this.duration)) {
         this.elapsedTime = this.elapsedTime + Gdx.graphics.getDeltaTime();
         this.alpha = Interpolation.smoother.apply(1.0F, 0.0F, Math.min(this.elapsedTime / this.duration, 1.0F));
         super.renderContext
            .drawLine(
               0.0F,
               0.0F,
               (float)WorldConfig.VIEWPORT_WIDTH,
               (float)WorldConfig.VIEWPORT_HEIGHT,
               new Color(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, this.alpha)
            );
         super.renderContext.resetBatchColor();
      }
   }
}
