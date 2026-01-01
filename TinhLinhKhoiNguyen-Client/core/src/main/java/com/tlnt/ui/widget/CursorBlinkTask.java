package com.tlnt.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Timer;

public final class CursorBlinkTask extends Timer.Task {
   public TextField textField;

   public CursorBlinkTask(TextField textField) {
      super();
      this.textField = textField;
   }

   @Override
   public final void run() {
      if (this.textField.getStage() == null) {
         this.cancel();
      } else {
         int cursorOn;
         if (!this.textField.cursorOn) {
            cursorOn = 1;
         } else {
            cursorOn = 0;
         }

         this.textField.cursorOn = (cursorOn != 0);
         Gdx.graphics.requestRendering();
      }
   }
}
