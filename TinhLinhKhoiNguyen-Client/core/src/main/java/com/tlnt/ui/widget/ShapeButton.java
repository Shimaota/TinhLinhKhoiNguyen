package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;

public class ShapeButton extends Button {
   public RenderContext renderContext;

   public ShapeButton(RenderContext renderContext) {
      super(new Button.ButtonStyle());
      this.renderContext = renderContext;
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      this.drawBackground();
      super.draw(batch, parentAlpha);
   }

   public void drawBackground() {
      ShapeDrawer drawer = this.renderContext.getShapeDrawer();
      Color fillColor = Color.LIGHT_GRAY;
      if (this.isDisabled()) {
         fillColor = Color.DARK_GRAY;
      } else if (this.isPressed()) {
         fillColor = Color.DARK_GRAY;
      } else if (this.isOver()) {
         fillColor = Color.GRAY;
      }

      drawer.drawFilledRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight(), fillColor);
      drawer.drawRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight(), Color.WHITE, 2.0F);
   }
}
