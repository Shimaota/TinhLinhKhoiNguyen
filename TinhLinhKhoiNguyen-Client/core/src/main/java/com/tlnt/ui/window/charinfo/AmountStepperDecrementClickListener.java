package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class AmountStepperDecrementClickListener extends ClickListener {
   public com.tlnt.ui.window.charinfo.AmountStepper amountStepper;
   public AmountStepperDecrementClickListener(AmountStepper stepper) {
      super();
      this.amountStepper = stepper;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.amountStepper.decrease();
   }
}
