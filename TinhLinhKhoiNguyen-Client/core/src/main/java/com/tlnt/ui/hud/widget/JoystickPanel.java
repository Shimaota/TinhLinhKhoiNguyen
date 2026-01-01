package com.tlnt.ui.hud.widget;

import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;

public final class JoystickPanel extends GamePanel {
   public MovementJoystick joystick;

   public JoystickPanel(RenderContext renderContext) {
      super(renderContext);
      this.setSize(282.0F, 282.0F);
      this.setPosition(50.0F, 47.0F);
      this.add(this.joystick = new MovementJoystick(this, 50.0F, super.renderContext)).grow();
      this.joystick.setKnobRadius(68.0F);
   }

   @Override
   public final void onUpdate(float delta) {
      InputHandler.getInstance().setMoveDirection(this.joystick.getKnobPercentX(), this.joystick.getKnobPercentY());
   }
}
