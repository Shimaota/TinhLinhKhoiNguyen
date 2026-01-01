package com.tlnt.ui.hud.widget;

import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.ui.widget.VirtualJoystick;

public final class MovementJoystick extends VirtualJoystick {
   public com.tlnt.ui.hud.widget.JoystickPanel baseOuterColor;

   public MovementJoystick(JoystickPanel joystickPanel, float deadzoneRadius, RenderContext renderContext) {
      super(50.0F, renderContext);
      this.baseOuterColor = joystickPanel;
   }

   @Override
   public final void drawBase(ShapeDrawer drawer, float cx, float cy, float baseRadius, float trackRadius, float deadzoneRadius, boolean touched, float alpha) {
      super.renderContext.drawTextureSized(TextureAsset.obtain("ui/31"), cx, cy, baseRadius * 2.0F, baseRadius * 2.0F, Anchor.CENTER);
   }

   @Override
   public final void drawKnob(ShapeDrawer drawer, float cx, float cy, float knobRadius, float alpha) {
      super.renderContext.drawTextureSized(TextureAsset.obtain("ui/30"), cx, cy, knobRadius * 2.0F, knobRadius * 2.0F, Anchor.CENTER);
   }

}
