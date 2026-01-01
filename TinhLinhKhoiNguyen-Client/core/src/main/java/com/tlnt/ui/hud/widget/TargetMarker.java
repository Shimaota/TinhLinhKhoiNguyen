package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.util.GameUtils;

public final class TargetMarker extends BaseTable {
   public TargetMarker.BadgeCondition target;

   public TargetMarker(RenderContext renderContext, TargetMarker.BadgeCondition target) {
      super(renderContext);
      this.target = target;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.target != null && this.target.hasBadge()) {
         ShapeDrawer drawer;
         (drawer = super.renderContext.getShapeDrawer())
            .drawFilledCircle(
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               GameUtils.oscillate(1.0F, this.getWidth() * 0.4F, this.getWidth() * 0.5F),
               Color.RED
            );
         drawer.setColor(Color.WHITE);
         drawer.drawCircle(
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            GameUtils.oscillate(1.0F, this.getWidth() * 0.4F, this.getWidth() * 0.5F),
            2.0F
         );
      }
   }

   public interface BadgeCondition {
      boolean hasBadge();
   }
}
