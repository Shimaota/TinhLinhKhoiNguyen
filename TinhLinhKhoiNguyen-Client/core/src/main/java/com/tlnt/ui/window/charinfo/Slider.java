package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Slider extends BaseTable {
   public NinePatchAsset trackBackground;
   public NinePatchAsset trackFilled;
   public TextureAsset thumbTexture;
   public float minValue;
   public float maxValue;
   public float stepSize;
   public float currentValue;
   public final List<Slider.SliderChangeListener> changeListeners;
   public final List<Slider.SliderDragListener> dragListeners;
   public boolean isDragging;
   public int handleOffset;

   public Slider(RenderContext renderContext) {
      super(renderContext);
      String trackPath = "ui/61";
      this.trackBackground = NinePatchAsset.obtain(trackPath, 9);
      this.trackFilled = NinePatchAsset.obtain("ui/60", 9);
      this.thumbTexture = TextureAsset.obtain("ui/62");
      this.minValue = 0.0F;
      this.maxValue = 100.0F;
      this.stepSize = 1.0F;
      this.currentValue = 0.0F;
      this.changeListeners = new ArrayList<>();
      this.dragListeners = new ArrayList<>();
      this.isDragging = false;
      this.handleOffset = -1;
      this.setTouchable(Touchable.enabled);
      this.addListener(new SliderInputListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      float x = this.getX();
      parentAlpha = this.getY();
      float width = this.getWidth();
      float height = this.getHeight();
      float fraction;
      fraction = (fraction = this.maxValue - this.minValue) == 0.0F ? 0.0F : MathUtils.clamp((this.currentValue - this.minValue) / fraction, 0.0F, 1.0F);
      super.renderContext.drawNinePatch(this.trackBackground, x, parentAlpha, width, height, Anchor.BOTTOM_LEFT);
      float filledWidth;
      if ((filledWidth = width * fraction) > 0.0F) {
         super.renderContext.drawNinePatch(this.trackFilled, x, parentAlpha, filledWidth, height, Anchor.BOTTOM_LEFT);
      }

      float thumbX = x + fraction * width;
      super.renderContext.drawTexture(this.thumbTexture, thumbX, parentAlpha + height / 2.0F, Anchor.CENTER);
   }

   public final void setValue(float value) {
      float oldValue = this.currentValue;
      value = clamp(value, this.minValue, this.maxValue);
      if (this.stepSize > 0.0F) {
         value = clamp((float)Math.round(value / this.stepSize) * this.stepSize, this.minValue, this.maxValue);
      }

      if (Float.compare(oldValue, value) != 0) {
         this.currentValue = value;
         Iterator it = new ArrayList<>(this.changeListeners).iterator();

         while (it.hasNext()) {
            ((Slider.SliderChangeListener)it.next()).onValueChanged(value);
         }
      }
   }

   public final void addValueChangeListener(Slider.SliderChangeListener listener) {
      this.changeListeners.add(listener);
   }

   public void setValueFromTouchPosition(float touchX) {
      touchX = MathUtils.clamp(this.getWidth() == 0.0F ? 0.0F : touchX / this.getWidth(), 0.0F, 1.0F);
      touchX = this.minValue + touchX * (this.maxValue - this.minValue);
      this.setValue(touchX);
   }

   public static float clamp(float value, float min, float max) {
      return value < min ? min : Math.min(value, max);
   }

   public interface SliderChangeListener {
      void onValueChanged(float value);
   }

   public interface SliderDragListener {
   }
}
