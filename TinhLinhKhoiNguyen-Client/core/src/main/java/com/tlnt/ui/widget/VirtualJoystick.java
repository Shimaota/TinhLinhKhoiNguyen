package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Pools;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;

public class VirtualJoystick extends Widget {
   public static Color baseOuterColor = new Color(0.1F, 0.1F, 0.12F, 0.85F);
   public static Color baseInnerColor = new Color(0.18F, 0.18F, 0.2F, 0.9F);
   public static Color borderColor = new Color(0.25F, 0.25F, 0.28F, 1.0F);
   public static Color activeBorderColor = new Color(0.2F, 0.6F, 1.0F, 1.0F);
   public static Color deadzoneFillColor = new Color(0.2F, 0.6F, 1.0F, 0.1F);
   public static Color knobColor = new Color(0.92F, 0.92F, 0.95F, 1.0F);
   public static Color knobBorderColor = new Color(0.18F, 0.18F, 0.2F, 1.0F);
   public final RenderContext renderContext;
   public boolean touched;
   public boolean resetOnTouchUp;
   public float deadzoneRadius;
   public boolean knobOvershootEnabled;
   public float knobOvershoot;
   public final Circle knobBounds;
   public final Circle touchBounds;
   public final Circle deadzoneBounds;
   public final Vector2 knobPosition;
   public final Vector2 knobPercent;
   public float baseRadius;
   public float trackRadius;
   public float knobRadius;
   public boolean autoSizeKnob;
   public float knobRadiusRatio;

   public VirtualJoystick(float deadzoneRadius, RenderContext renderContext) {
      this.resetOnTouchUp = true;
      this.knobOvershootEnabled = true;
      this.knobOvershoot = 0.0F;
      this.knobBounds = new Circle(0.0F, 0.0F, 0.0F);
      this.touchBounds = new Circle(0.0F, 0.0F, 0.0F);
      this.deadzoneBounds = new Circle(0.0F, 0.0F, 0.0F);
      this.knobPosition = new Vector2();
      this.knobPercent = new Vector2();
      this.autoSizeKnob = true;
      this.knobRadiusRatio = 0.35F;
      if (deadzoneRadius < 0.0F) {
         throw new IllegalArgumentException("deadzoneRadius must be > 0");
      } else {
         this.deadzoneRadius = deadzoneRadius;
         this.renderContext = renderContext;
         this.setSize(this.getPrefWidth(), this.getPrefHeight());
         this.addListener(new JoystickInputListener(this));
      }
   }

   public final void calculatePositionAndValue(float x, float y, boolean reset) {
      float oldKnobX = this.knobPosition.x;
      float oldKnobY = this.knobPosition.y;
      float oldPercentX = this.knobPercent.x;
      float oldPercentY = this.knobPercent.y;
      float centerX = this.knobBounds.x;
      float centerY = this.knobBounds.y;
      this.knobPercent.set(0.0F, 0.0F);
      this.knobPosition.set(centerX, centerY);
      if (!reset) {
         float percentX = (x - centerX) / this.trackRadius;
         float percentY = (y - centerY) / this.trackRadius;
         if (!this.deadzoneBounds.contains(x, y)) {
            this.knobPercent.set(percentX, percentY);
            if ((x = this.knobPercent.len()) > 1.0F) {
               this.knobPercent.scl(1.0F / x);
            }

            x = this.knobOvershootEnabled ? this.trackRadius + Math.max(0.0F, this.knobOvershoot) : Math.max(0.0F, this.trackRadius - this.knobRadius);
            this.knobPosition.set(this.knobPercent).scl(x).add(centerX, centerY);
         }
      }

      if (oldPercentX != this.knobPercent.x || oldPercentY != this.knobPercent.y) {
         ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
         if (this.fire(changeEvent)) {
            this.knobPercent.set(oldPercentX, oldPercentY);
            this.knobPosition.set(oldKnobX, oldKnobY);
         }

         Pools.free(changeEvent);
      }
   }

   @Override
   public void layout() {
      float centerX = this.getWidth() * 0.5F;
      float centerY = this.getHeight() * 0.5F;
      this.baseRadius = Math.min(centerX, centerY);
      this.trackRadius = this.baseRadius * 0.9F;
      if (this.autoSizeKnob) {
         this.knobRadius = Math.max(4.0F, this.baseRadius * this.knobRadiusRatio);
      }

      this.touchBounds.set(centerX, centerY, this.trackRadius);
      float knobRange = this.knobOvershootEnabled ? this.trackRadius + Math.max(0.0F, this.knobOvershoot) : Math.max(0.0F, this.trackRadius - this.knobRadius);
      this.knobBounds.set(centerX, centerY, Math.max(0.0F, knobRange));
      this.deadzoneBounds.set(centerX, centerY, this.deadzoneRadius);
      if (!this.touched) {
         this.knobPosition.set(centerX, centerY);
         this.knobPercent.set(0.0F, 0.0F);
      } else {
         Vector2 offset;
         float dist = (offset = new Vector2(this.knobPosition.x - centerX, this.knobPosition.y - centerY)).len();
         float maxDist = this.knobBounds.radius;
         if (dist > maxDist && dist > 0.0F) {
            offset.scl(maxDist / dist);
            this.knobPosition.set(centerX + offset.x, centerY + offset.y);
         }

         knobRange = Math.max(1.0E-4F, this.trackRadius);
         this.knobPercent.set((this.knobPosition.x - centerX) / knobRange, (this.knobPosition.y - centerY) / knobRange);
         if ((centerX = this.knobPercent.len()) > 1.0F) {
            this.knobPercent.scl(1.0F / centerX);
         }
      }
   }

   @Override
   public float getPrefWidth() {
      return 160.0F;
   }

   @Override
   public float getPrefHeight() {
      return 160.0F;
   }

   @Override
   public Actor hit(float x, float y, boolean touchable) {
      if (touchable && this.getTouchable() != Touchable.enabled) {
         return null;
      } else if (!this.isVisible()) {
         return null;
      } else {
         return this.touchBounds.contains(x, y) ? this : null;
      }
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      this.validate();
      ShapeDrawer drawer = this.renderContext.getShapeDrawer();
      parentAlpha = this.getColor().a * parentAlpha;
      float x = this.getX();
      float y = this.getY();
      float centerX = x + this.knobBounds.x;
      float centerY = y + this.knobBounds.y;
      this.drawBase(drawer, centerX, centerY, this.baseRadius, this.trackRadius, this.deadzoneRadius, this.touched, parentAlpha);
      x += this.knobPosition.x;
      y += this.knobPosition.y;
      this.drawKnob(drawer, x, y, this.knobRadius, parentAlpha);
   }

   public void drawBase(ShapeDrawer drawer, float cx, float cy, float baseRadius, float trackRadius, float deadzoneRadius, boolean touched, float alpha) {
      drawer.drawFilledCircle(cx, cy, baseRadius, new Color(baseOuterColor.r, baseOuterColor.g, baseOuterColor.b, baseOuterColor.a * alpha));
      drawer.drawFilledCircle(cx, cy, trackRadius, new Color(baseInnerColor.r, baseInnerColor.g, baseInnerColor.b, baseInnerColor.a * alpha));
      Color borderCol = touched ? activeBorderColor : borderColor;
      drawer.setColor(borderCol);
      drawer.drawCircle(cx, cy, baseRadius, 2.0F);
      if (deadzoneRadius > 0.0F) {
         drawer.drawFilledCircle(cx, cy, deadzoneRadius, new Color(deadzoneFillColor.r, deadzoneFillColor.g, deadzoneFillColor.b, deadzoneFillColor.a * alpha));
      }
   }

   public void drawKnob(ShapeDrawer drawer, float cx, float cy, float knobRadius, float alpha) {
      float shadowRadius = knobRadius * 1.05F;
      drawer.drawFilledCircle(cx + 2.0F, cy - 2.0F, shadowRadius, new Color(0.0F, 0.0F, 0.0F, 0.22F * alpha));
      drawer.drawFilledCircle(cx, cy, knobRadius, new Color(knobColor.r, knobColor.g, knobColor.b, knobColor.a * alpha));
      drawer.setColor(new Color(knobBorderColor.r, knobBorderColor.g, knobBorderColor.b, knobBorderColor.a * alpha));
      drawer.drawCircle(cx, cy, knobRadius, 2.0F);
   }

   public final float getKnobPercentX() {
      return this.knobPercent.x;
   }

   public final float getKnobPercentY() {
      return this.knobPercent.y;
   }

   public final void setKnobRadius(float radius) {
      this.autoSizeKnob = false;
      this.knobRadius = Math.max(4.0F, 68.0F);
      this.invalidate();
   }

}
