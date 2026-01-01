package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.Vector2;

public abstract class Drawer<T extends BatchManager> {
   public static final Vector2 VEC_A = new Vector2();
   public static final Vector2 VEC_B = new Vector2();
   public static final Vector2 VEC_C = new Vector2();
   public static final Vector2 VEC_D = new Vector2();
   public static final Vector2 VEC_E = new Vector2();
   public static final Vector2 DIR = new Vector2();
   public static final Vector2 VEC_1 = new Vector2();
   public final AbstractShapeDrawer drawer;
   public final T batchManager;

   public Drawer(T batchManager, AbstractShapeDrawer drawer) {
      this.drawer = drawer;
      this.batchManager = (T)batchManager;
   }

   public final void drawSmoothJoinFill(Vector2 A, Vector2 B, Vector2 C, Vector2 D, Vector2 E, float halfLineWidth) {
      this.batchManager.ensureSpaceForTriangle();
      boolean bendsLeft = GeometryUtils.prepareSmoothJoin(A, B, C, D, E, halfLineWidth, false);
      this.setVertex1(bendsLeft ? E : D);
      this.setVertex2(bendsLeft ? D : E);
      bendsLeft = GeometryUtils.prepareSmoothJoin(A, B, C, D, E, halfLineWidth, true);
      this.setVertex3(bendsLeft ? E : D);
      float c = this.batchManager.floatBits;
      this.setColors(c, c, c);
      this.batchManager.pushTriangle();
   }

   public final void drawSmoothJoinFill(Vector2 A, Vector2 B, Vector2 C, Vector2 D, Vector2 E, Vector2 offset, float cos, float sin, float halfLineWidth) {
      this.batchManager.ensureSpaceForTriangle();
      boolean bendsLeft;
      Vector2 V1 = (bendsLeft = GeometryUtils.prepareSmoothJoin(A, B, C, D, E, halfLineWidth, false))
         ? E
         : D;
      Vector2 V2 = bendsLeft ? D : E;
      this.setVertex1(V1.x * cos - V1.y * sin + offset.x, V1.x * sin + V1.y * cos + offset.y);
      this.setVertex2(V2.x * cos - V2.y * sin + offset.x, V2.x * sin + V2.y * cos + offset.y);
      Vector2 V3;
      float x3 = (V3 = GeometryUtils.prepareSmoothJoin(A, B, C, D, E, halfLineWidth, true) ? E : D).x
            * cos
         - V3.y * sin
         + offset.x;
      float y3 = V3.x * sin + V3.y * cos + offset.y;
      this.setVertex3(x3, y3);
      float c = this.batchManager.floatBits;
      this.setColors(c, c, c);
      this.batchManager.pushTriangle();
   }

   public final void setX1(float x) {
      this.batchManager.setX1(x);
   }

   public final void setY1(float y) {
      this.batchManager.setY1(y);
   }

   public final void setX2(float x) {
      this.batchManager.setX2(x);
   }

   public final void setY2(float y) {
      this.batchManager.setY2(y);
   }

   public final void setX3(float x) {
      this.batchManager.setX3(x);
   }

   public final void setY3(float y) {
      this.batchManager.setY3(y);
   }

   public final void setX4(float x) {
      this.batchManager.setX4(x);
   }

   public final void setY4(float y) {
      this.batchManager.setY4(y);
   }

   public final void setVertex1(float x, float y) {
      this.setX1(x);
      this.setY1(y);
   }

   public final void setVertex2(float x, float y) {
      this.setX2(x);
      this.setY2(y);
   }

   public final void setVertex3(float x, float y) {
      this.setX3(x);
      this.setY3(y);
   }

   public final void setVertex4(float x, float y) {
      this.setX4(x);
      this.setY4(y);
   }

   public final void setVertex1(Vector2 v) {
      this.setVertex1(v.x, v.y);
   }

   public final void setVertex2(Vector2 v) {
      this.setVertex2(v.x, v.y);
   }

   public final void setVertex3(Vector2 v) {
      this.setVertex3(v.x, v.y);
   }

   public final void setVertex4(Vector2 v) {
      this.setVertex4(v.x, v.y);
   }

   public final void setColor1(float c) {
      this.batchManager.setColor1(c);
   }

   public final void setColor2(float c) {
      this.batchManager.setColor2(c);
   }

   public final void setColor3(float c) {
      this.batchManager.setColor3(c);
   }

   public final void setColor4(float c) {
      this.batchManager.setColor4(c);
   }

   public final void setColors(float c1, float c2, float c3) {
      this.setColor1(c1);
      this.setColor2(c2);
      this.setColor3(c3);
   }

   public final void setColors(float c1, float c2, float c3, float c4) {
      this.setColor1(c1);
      this.setColor2(c2);
      this.setColor3(c3);
      this.setColor4(c4);
   }

   public final float getX3() {
      return this.batchManager.getX3();
   }

   public final float getY3() {
      return this.batchManager.getY3();
   }

   public final float getX4() {
      return this.batchManager.getX4();
   }

   public final float getY4() {
      return this.batchManager.getY4();
   }
}
