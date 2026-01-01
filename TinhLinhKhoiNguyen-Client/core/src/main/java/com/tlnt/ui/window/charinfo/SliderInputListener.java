package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import java.util.ArrayList;

public final class SliderInputListener extends InputListener {
   public com.tlnt.ui.window.charinfo.Slider slider;
   public SliderInputListener(Slider slider) {
      super();
      this.slider = slider;
   }

   @Override
   public final boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
      Slider target = this.slider;
      int inside;
      if (x >= 0.0F && x <= target.getWidth() && y >= 0.0F && y <= target.getHeight()) {
         inside = 1;
      } else {
         inside = 0;
      }

      if (inside == 0) {
         return false;
      } else {
         this.slider.isDragging = true;
         this.slider.handleOffset = pointer;
         this.slider.setValueFromTouchPosition(x);

         for (Slider.SliderDragListener listener : new ArrayList<>(this.slider.dragListeners)) {
            ;
         }

         return true;
      }
   }

   @Override
   public final void touchDragged(InputEvent event, float x, float y, int pointer) {
      if (this.slider.isDragging && pointer == this.slider.handleOffset) {
         this.slider.setValueFromTouchPosition(x);

         for (Slider.SliderDragListener listener : new ArrayList<>(this.slider.dragListeners)) {
            ;
         }
      }
   }

   @Override
   public final void touchUp(InputEvent event, float x, float y, int pointer, int button) {
      if (this.slider.isDragging && pointer == this.slider.handleOffset) {
         this.slider.isDragging = false;
         this.slider.handleOffset = -1;
         this.slider.setValueFromTouchPosition(x);

         for (Slider.SliderDragListener listener : new ArrayList<>(this.slider.dragListeners)) {
            ;
         }
      }
   }
}
