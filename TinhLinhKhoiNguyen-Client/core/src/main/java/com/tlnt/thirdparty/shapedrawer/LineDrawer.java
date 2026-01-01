package com.tlnt.thirdparty.shapedrawer;

public final class LineDrawer extends Drawer<BatchManager> {
   public LineDrawer(BatchManager batchManager, AbstractShapeDrawer drawer) {
      super(batchManager, drawer);
   }

   public final void drawLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap) {
      this.drawLine(x1, y1, x2, y2, lineWidth, false, super.batchManager.floatBits, super.batchManager.floatBits);
   }

   public final void drawLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap, float c1, float c2) {
      this.pushLine(x1, y1, x2, y2, lineWidth, snap, c1, c2);
      super.batchManager.pushToBatch();
   }

   public final void pushLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap) {
      this.pushLine(x1, y1, x2, y2, lineWidth, false, super.batchManager.floatBits, super.batchManager.floatBits);
   }

   public void pushLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap, float c1, float c2) {
      super.batchManager.ensureSpaceForQuad();
      float xDiff = 0.0F;
      float yDiff = 0.0F;
      float dx = x2 - x1;
      float dy = y2 - y1;
      if (snap) {
         float offset = super.batchManager.offset;
         float pixelSize = super.batchManager.pixelSize;
         float halfPixelSize = super.batchManager.halfPixelSize;
         x1 = ShapeUtils.snapToPixel(x1, pixelSize, halfPixelSize) - Math.signum(dx) * offset;
         y1 = ShapeUtils.snapToPixel(y1, pixelSize, halfPixelSize) - Math.signum(dy) * offset;
         x2 = ShapeUtils.snapToPixel(x2, pixelSize, halfPixelSize) + Math.signum(dx) * offset;
         y2 = ShapeUtils.snapToPixel(y2, pixelSize, halfPixelSize) + Math.signum(dy) * offset;
      }

      float halfLineWidth = 0.5F * lineWidth;
      if (x1 == x2) {
         xDiff = halfLineWidth;
      } else if (y1 == y2) {
         yDiff = halfLineWidth;
      } else {
         xDiff = y2 - y1;
         yDiff = x2 - x1;
         float lengthSq = xDiff * xDiff + yDiff * yDiff;
         lineWidth = 1.0F / (float)Math.sqrt((double)lengthSq) * halfLineWidth;
         xDiff *= lineWidth;
         yDiff *= lineWidth;
      }

      this.setX1(x1 + xDiff);
      this.setY1(y1 - yDiff);
      this.setX2(x1 - xDiff);
      this.setY2(y1 + yDiff);
      this.setX3(x2 - xDiff);
      this.setY3(y2 + yDiff);
      this.setX4(x2 + xDiff);
      this.setY4(y2 - yDiff);
      this.setColor1(c1);
      this.setColor2(c1);
      this.setColor3(c2);
      this.setColor4(c2);
      super.batchManager.pushQuad();
      if (!super.batchManager.isCachingDraws()) {
         super.batchManager.pushToBatch();
      }
   }
}
