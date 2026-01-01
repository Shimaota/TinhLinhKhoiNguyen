package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.EarClippingTriangulator;

public abstract class FilledPolygonDrawer<T extends BatchManager> extends Drawer<T> {
   public FilledPolygonDrawer(T batchManager, AbstractShapeDrawer drawer) {
      super((T)batchManager, drawer);
   }

   public abstract void drawPolygon(float centreX, float centreY, int sides, float radiusX, float radiusY, float rotation, float startAngle, float radians, float innerColor, float outerColor);

   public final void drawRectangle(float x, float y, float width, float height, float rotation) {
      float color = super.batchManager.floatBits;
      boolean wasCaching = super.batchManager.isCachingDraws();
      super.batchManager.ensureSpaceForQuad();
      float cos = (float)Math.cos((double)rotation);
      float sin = (float)Math.sin((double)rotation);
      float halfWidth = 0.5F * width;
      rotation = 0.5F * height;
      float centreX = x + halfWidth;
      width = y + rotation;
      this.setX1(halfWidth * cos - rotation * sin + centreX);
      this.setY1(halfWidth * sin + rotation * cos + width);
      this.setX2(-halfWidth * cos - rotation * sin + centreX);
      this.setY2(-halfWidth * sin + rotation * cos + width);
      this.setX3(-halfWidth * cos - -rotation * sin + centreX);
      this.setY3(-halfWidth * sin + -rotation * cos + width);
      this.setX4(halfWidth * cos - -rotation * sin + centreX);
      this.setY4(halfWidth * sin + -rotation * cos + width);
      this.setColors(color, color, color, color);
      super.batchManager.pushQuad();
      if (!wasCaching) {
         super.batchManager.pushToBatch();
      }
   }

   public final void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float color1, float color2, float color3) {
      boolean wasCaching = super.batchManager.isCachingDraws();
      this.setVertex1(x1, y1);
      this.setVertex2(x2, y2);
      this.setVertex3(x3, y3);
      this.setColors(color1, color2, color3);
      super.batchManager.pushTriangle();
      if (!wasCaching) {
         super.batchManager.pushToBatch();
      }
   }

   static {
      new EarClippingTriangulator();
   }

   public static class BatchedFilledPolygonDrawer extends FilledPolygonDrawer<BatchManager> {
      public BatchedFilledPolygonDrawer(BatchManager batchManager, AbstractShapeDrawer drawer) {
         super(batchManager, drawer);
      }

      @Override
      public final void drawPolygon(float centreX, float centreY, int sides, float radiusX, float radiusY, float rotation, float startAngle, float radians, float innerColor, float outerColor) {
         radians = Math.min((float) (Math.PI * 2), (float) (Math.PI * 2));
         boolean wasCaching = super.batchManager.startCaching();
         float angleInterval = (float) (Math.PI * 2) / (float)sides;
         radians += 0.0F;
         float cos = (float)Math.cos((double)angleInterval);
         float sin = (float)Math.sin((double)angleInterval);
         float cosRot = (float)Math.cos((double)rotation);
         rotation = (float)Math.sin((double)rotation);
         int start = (int)Math.ceil((double)((float)sides * (0.0F / (float) (Math.PI * 2))));
         sides = (int)Math.floor((double)((float)sides * (radians / (float) (Math.PI * 2)))) + 1;
         if (ShapeUtils.epsilonEquals((float)start * angleInterval, 0.0F)) {
            start++;
         }

         VEC_B.set(1.0F, 0.0F).rotateRad(0.0F).scl(radiusX, radiusY);
         int n = sides - start;
         n = sides = n + 1;
         if (n < 2) {
            super.batchManager.ensureSpaceForTriangle();
            VEC_A.set(1.0F, 0.0F).rotateRad(0.0F).scl(radiusX, radiusY);
            VEC_B.set(1.0F, 0.0F).rotateRad(radians).scl(radiusX, radiusY);
            this.setVertex1(centreX, centreY);
            this.setX2(VEC_A.x * cosRot - VEC_A.y * rotation + centreX);
            this.setY2(VEC_A.x * rotation + VEC_A.y * cosRot + centreY);
            this.setX3(VEC_B.x * cosRot - VEC_B.y * rotation + centreX);
            this.setY3(VEC_B.x * rotation + VEC_B.y * cosRot + centreY);
            this.setColors(innerColor, outerColor, outerColor);
            super.batchManager.pushTriangle();
         } else {
            DIR.set(1.0F, 0.0F).rotateRad(Math.min((float)start * angleInterval, radians));
            VEC_C.set(DIR).scl(radiusX, radiusY);
         }

         int i = 0;

         while (true) {
            if (i >= sides - 1) {
               if (!wasCaching) {
                  super.batchManager.endCaching();
               }

               return;
            }

            VEC_A.set(VEC_B);
            VEC_B.set(VEC_C);
            if (i < sides - 2) {
               DIR.set(DIR.x * cos - DIR.y * sin, DIR.x * sin + DIR.y * cos);
               VEC_C.set(DIR).scl(radiusX, radiusY);
            } else {
               VEC_C.set(1.0F, 0.0F).rotateRad(radians).scl(radiusX, radiusY);
            }

            if (i % 2 == 0) {
               super.batchManager.ensureSpaceForQuad();
               this.setVertex1(centreX, centreY);
               this.setX2(VEC_A.x * cosRot - VEC_A.y * rotation + centreX);
               this.setY2(VEC_A.x * rotation + VEC_A.y * cosRot + centreY);
               this.setX3(VEC_B.x * cosRot - VEC_B.y * rotation + centreX);
               this.setY3(VEC_B.x * rotation + VEC_B.y * cosRot + centreY);
               this.setX4(VEC_C.x * cosRot - VEC_C.y * rotation + centreX);
               this.setY4(VEC_C.x * rotation + VEC_C.y * cosRot + centreY);
               this.setColors(innerColor, outerColor, outerColor, outerColor);
               super.batchManager.pushQuad();
            } else if (i == sides - 2) {
               super.batchManager.ensureSpaceForTriangle();
               VEC_C.set(1.0F, 0.0F).rotateRad(radians).scl(radiusX, radiusY);
               this.setVertex1(centreX, centreY);
               this.setX2(VEC_B.x * cosRot - VEC_B.y * rotation + centreX);
               this.setY2(VEC_B.x * rotation + VEC_B.y * cosRot + centreY);
               this.setX3(VEC_C.x * cosRot - VEC_C.y * rotation + centreX);
               this.setY3(VEC_C.x * rotation + VEC_C.y * cosRot + centreY);
               this.setColors(innerColor, outerColor, outerColor);
               super.batchManager.pushTriangle();
            }

            i++;
         }
      }
   }

   public static class PolygonBatchedFilledPolygonDrawer extends FilledPolygonDrawer<PolygonBatchManager> {
      public PolygonBatchedFilledPolygonDrawer(PolygonBatchManager batchManager, AbstractShapeDrawer drawer) {
         super(batchManager, drawer);
      }

      @Override
      public final void drawPolygon(float centreX, float centreY, int sides, float radiusX, float radiusY, float rotation, float startAngle, float radians, float innerColor, float outerColor) {
         radians = Math.min((float) (Math.PI * 2), (float) (Math.PI * 2));
         boolean wasCaching = ((PolygonBatchManager)super.batchManager).startCaching();
         float angleInterval = (float) (Math.PI * 2) / (float)sides;
         radians += 0.0F;
         float cos = (float)Math.cos((double)angleInterval);
         float sin = (float)Math.sin((double)angleInterval);
         float cosRot = (float)Math.cos((double)rotation);
         rotation = (float)Math.sin((double)rotation);
         int start = (int)Math.ceil((double)((float)sides * (0.0F / (float) (Math.PI * 2))));
         sides = (int)Math.floor((double)((float)sides * (radians / (float) (Math.PI * 2)))) + 1;
         if (ShapeUtils.epsilonEquals((float)start * angleInterval, 0.0F)) {
            start++;
         }

         sides = sides - start + 1;
         ((PolygonBatchManager)super.batchManager).ensureSpace(sides + 2);
         int vertexOffset = ((PolygonBatchManager)super.batchManager).getVerticesArrayIndex();
         this.setVertex1(centreX, centreY);
         this.setColor1(innerColor);
         ((PolygonBatchManager)super.batchManager).pushVertex();
         VEC_A.set(1.0F, 0.0F).rotateRad(0.0F).scl(radiusX, radiusY);
         this.setX1(VEC_A.x * cosRot - VEC_A.y * rotation + centreX);
         this.setY1(VEC_A.x * rotation + VEC_A.y * cosRot + centreY);
         this.setColor1(outerColor);
         ((PolygonBatchManager)super.batchManager).pushVertex();
         ((PolygonBatchManager)super.batchManager)
            .pushTriangleIndices((short)vertexOffset, (short)(vertexOffset + 1), (short)(vertexOffset + 2));
         DIR.set(1.0F, 0.0F).rotateRad(Math.min((float)start * angleInterval, radians));
         VEC_A.set(DIR).scl(radiusX, radiusY);
         int i = 0;

         while (true) {
            if (i >= sides - 1) {
               VEC_A.set(1.0F, 0.0F).rotateRad(radians).scl(radiusX, radiusY);
               this.setX1(VEC_A.x * cosRot - VEC_A.y * rotation + centreX);
               this.setY1(VEC_A.x * rotation + VEC_A.y * cosRot + centreY);
               this.setColor1(outerColor);
               ((PolygonBatchManager)super.batchManager).pushVertex();
               if (!wasCaching) {
                  ((PolygonBatchManager)super.batchManager).endCaching();
               }

               return;
            }

            this.setX1(VEC_A.x * cosRot - VEC_A.y * rotation + centreX);
            this.setY1(VEC_A.x * rotation + VEC_A.y * cosRot + centreY);
            this.setColor1(outerColor);
            ((PolygonBatchManager)super.batchManager).pushVertex();
            DIR.set(DIR.x * cos - DIR.y * sin, DIR.x * sin + DIR.y * cos);
            VEC_A.set(DIR).scl(radiusX, radiusY);
            PolygonBatchManager polygonBatchManager = (PolygonBatchManager)super.batchManager;
            short centreIndex = (short)vertexOffset;
            int offset1 = vertexOffset + i;
            short secondIndex = (short)(offset1 + 2);
            int offset2 = vertexOffset + i;
            polygonBatchManager.pushTriangleIndices(centreIndex, secondIndex, (short)(offset2 + 3));
            i++;
         }
      }
   }
}
