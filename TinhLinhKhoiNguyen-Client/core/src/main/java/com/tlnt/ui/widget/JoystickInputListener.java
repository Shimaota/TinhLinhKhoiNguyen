package com.tlnt.ui.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class JoystickInputListener extends InputListener {
   public com.tlnt.ui.widget.VirtualJoystick joystick;
   public JoystickInputListener(VirtualJoystick joystick) {
      super();
      this.joystick = joystick;
   }

   @Override
   public final boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
      if (this.joystick.touched) {
         return false;
      } else {
         this.joystick.touched = true;
         this.joystick.calculatePositionAndValue(x, y, false);
         return true;
      }
   }

   @Override
   public final void touchDragged(InputEvent event, float x, float y, int pointer) {
      this.joystick.calculatePositionAndValue(x, y, false);
   }

   @Override
   public final void touchUp(InputEvent event, float x, float y, int pointer, int button) {
      this.joystick.touched = false;
      this.joystick.calculatePositionAndValue(x, y, this.joystick.resetOnTouchUp);
   }
}
