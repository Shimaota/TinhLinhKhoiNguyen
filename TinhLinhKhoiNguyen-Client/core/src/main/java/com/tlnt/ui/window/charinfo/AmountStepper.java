package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class AmountStepper extends BaseTable {
   public TextureAsset backgroundTexture;
   public String labelText;
   public float minValue;
   public float maxValue;
   public float stepSize;
   public float currentValue;
   public AmountStepper.ValueChangeListener valueChangeListener;
   public DecrementButton decreaseButton;
   public AmountStepperIncreaseButton increaseButton;

   public AmountStepper(String label, float min, float max, float step, RenderContext renderContext) {
      super(renderContext);
      this.backgroundTexture = TextureAsset.obtain("ui/137");
      this.labelText = label;
      this.minValue = 0.0F;
      this.maxValue = 100.0F;
      this.stepSize = 5.0F;
      this.decreaseButton = new DecrementButton(this, super.renderContext);
      this.add(this.decreaseButton).size(55.0F);
      this.decreaseButton.addListener(new AmountStepperDecrementClickListener(this));
      this.add().grow();
      this.increaseButton = new AmountStepperIncreaseButton(this, super.renderContext);
      this.add(this.increaseButton).size(55.0F);
      this.increaseButton.addListener(new AmountIncrementListener(this));
   }

   public final void setValueChangeListener(AmountStepper.ValueChangeListener listener) {
      this.valueChangeListener = listener;
   }

   public final float getValue() {
      return this.currentValue;
   }

   public final void setValue(float value) {
      this.currentValue = value;
   }

   public void decrease() {
      this.currentValue = this.currentValue - this.stepSize;
      if (this.currentValue < this.minValue) {
         this.currentValue = this.minValue;
      }

      if (this.valueChangeListener != null) {
         this.valueChangeListener.onValueChanged(this.currentValue);
      }
   }

   public void increase() {
      this.currentValue = this.currentValue + this.stepSize;
      if (this.currentValue > this.maxValue) {
         this.currentValue = this.maxValue;
      }

      if (this.valueChangeListener != null) {
         this.valueChangeListener.onValueChanged(this.currentValue);
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String text = this.labelText;
      float textX = this.getX();
      float textY = this.getY() + this.getHeight() + 23.0F;
      ctx.drawTextColorName(font, text, textX, textY, Anchor.BOTTOM_LEFT, 24.0F, "#8B4E23");
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX() + 63.0F, this.getY() - 3.0F, 165.0F, 60.0F, Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            this.currentValue + "%",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

   public interface ValueChangeListener {
      void onValueChanged(float value);
   }
}
