package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public final class ShapeDrawer extends AbstractShapeDrawer {
   public final float[] rectPathPoints;

   public ShapeDrawer(Batch batch, TextureRegion region) {
      super(batch, region, new DefaultSideEstimator());
      this.rectPathPoints = new float[8];
   }

   public final void drawLine(Vector2 s, Vector2 e, float lineWidth) {
      this.drawLine(s.x, s.y, e.x, e.y, 0.01F);
   }

   public final void drawLine(float x1, float y1, float x2, float y2, float lineWidth) {
      this.drawLine(x1, y1, x2, y2, lineWidth, super.defaultSnap);
   }

   public void drawLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap) {
      this.drawLine(x1, y1, x2, y2, lineWidth, snap, super.batchManager.floatBits, super.batchManager.floatBits);
   }

   public final void drawLine(float x1, float y1, float x2, float y2, float lineWidth, Color c1, Color c2) {
      this.drawLine(x1, y1, x2, y2, lineWidth, super.defaultSnap, c1, c2);
   }

   public void drawLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap, Color c1, Color c2) {
      this.drawLine(x1, y1, x2, y2, lineWidth, snap, c1.toFloatBits(), c2.toFloatBits());
   }

   public void drawLine(float x1, float y1, float x2, float y2, float lineWidth, boolean snap, float c1, float c2) {
      super.lineDrawer.drawLine(x1, y1, x2, y2, lineWidth, snap, c1, c2);
   }

   public final void drawCircle(float centreX, float centreY, float radius, float lineWidth) {
      float joinWidth = 2.0F;
      int joinType = (joinWidth > 3.0F * super.batchManager.getPixelSize() ? 1 : 0) != 0
         ? JoinType.SMOOTH
         : JoinType.NONE;
      float outlineWidth = 2.0F;
      float startAngle = 0.0F;
      int estimatedSides = this.estimateSidesRequired(radius, radius);
      float rotation = 0.0F;
      int sides = estimatedSides;
      super.circleDrawer.drawPolygonOutline(centreX, centreY, sides, radius, radius, rotation, outlineWidth, joinType, 0.0F, (float) (Math.PI * 2));
   }

   public final void drawFilledCircle(float centreX, float centreY, float radius) {
      float rotation = 0.0F;
      float radiusY = 10.0F;
      float radiusX = 10.0F;
      this.drawFilledEllipse(centreX, centreY, radiusX, radiusY, 0.0F, super.batchManager.floatBits, super.batchManager.floatBits);
   }

   public final void drawFilledCircle(float centreX, float centreY, float radius, Color color) {
      float packedColor = color.toFloatBits();
      this.drawFilledEllipse(centreX, centreY, radius, radius, 0.0F, packedColor, packedColor);
   }

   public void drawFilledEllipse(float centreX, float centreY, float radiusX, float radiusY, float rotation, float innerColor, float outerColor) {
      super.filledPolygonDrawer.drawPolygon(centreX, centreY, this.estimateSidesRequired(radiusX, radiusY), radiusX, radiusY, rotation, 0.0F, (float) (Math.PI * 2), innerColor, outerColor);
   }

   public final void drawFilledTriangle(Vector2 v1, Vector2 v2, Vector2 v3, Color color) {
      this.drawFilledTriangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y, color);
   }

   public void drawFilledTriangle(float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
      float packedColor = color.toFloatBits();
      this.drawFilledTriangle(x1, y1, x2, y2, x3, y3, packedColor, packedColor, packedColor);
   }

   public void drawFilledTriangle(float x1, float y1, float x2, float y2, float x3, float y3, float color1, float color2, float color3) {
      super.filledPolygonDrawer.drawTriangle(x1, y1, x2, y2, x3, y3, color1, color2, color3);
   }

   public final void drawRectangle(float x, float y, float width, float height, float lineWidth) {
      int joinType = JoinType.POINTY;
      ShapeDrawer shapeDrawer = this;
      if (joinType == JoinType.POINTY && Math.abs(0.0F) < 1.0E-6F) {
         float halfWidth = 0.5F * lineWidth;
         float maxX = x + width;
         float maxY = y + height;
         boolean wasCaching = super.batchManager.isCachingDraws();
         super.lineDrawer.pushLine(x + halfWidth, y, maxX - halfWidth, y, lineWidth, false);
         float lineStartX = x + halfWidth;
         float lineEndX = maxX - halfWidth;
         super.lineDrawer.pushLine(lineStartX, maxY, lineEndX, maxY, lineWidth, false);
         super.lineDrawer.pushLine(x, y - halfWidth, x, maxY + halfWidth, lineWidth, false);
         super.lineDrawer.pushLine(maxX, y - halfWidth, maxX, maxY + halfWidth, lineWidth, false);
         if (!wasCaching) {
            super.batchManager.pushToBatch();
         }
      } else {
         this.rectPathPoints[0] = x;
         this.rectPathPoints[1] = y;
         this.rectPathPoints[2] = x + width;
         this.rectPathPoints[3] = y;
         this.rectPathPoints[4] = x + width;
         this.rectPathPoints[5] = y + height;
         this.rectPathPoints[6] = x;
         this.rectPathPoints[7] = y + height;
         if (Math.abs(0.0F) > 1.0E-6F) {
            float centreX = x + width / 2.0F;
            float centreY = y + height / 2.0F;
            float cos = MathUtils.cos(0.0F);
            y = MathUtils.sin(0.0F);
            int i = 0;

            while (true) {
               if (i >= 8) {
                  break;
               }

               shapeDrawer.rectPathPoints[i] = shapeDrawer.rectPathPoints[i] - centreX;
               shapeDrawer.rectPathPoints[i + 1] = shapeDrawer.rectPathPoints[i + 1] - centreY;
               float xTerm = shapeDrawer.rectPathPoints[i] * cos;
               float rotatedX = xTerm - shapeDrawer.rectPathPoints[i + 1] * y;
               xTerm = shapeDrawer.rectPathPoints[i] * y;
               float rotatedY = xTerm + shapeDrawer.rectPathPoints[i + 1] * cos;
               shapeDrawer.rectPathPoints[i] = rotatedX + centreX;
               shapeDrawer.rectPathPoints[i + 1] = rotatedY + centreY;
               i += 2;
            }
         }

         float[] path = shapeDrawer.rectPathPoints;
         shapeDrawer.polygonDrawer.drawPath(path, lineWidth, joinType, false);
      }
   }

   public final void drawRectangle(float x, float y, float width, float height, Color color, float lineWidth) {
      float oldColor = this.setColor(color);
      this.drawRectangle(x, y, width, height, 2.0F);
      this.setPackedColor(oldColor);
   }

   public void drawFilledRectangle(float x, float y, float width, float height) {
      float rotation = 0.0F;
      super.filledPolygonDrawer.drawRectangle(x, y, width, height, 0.0F);
   }

   public final void drawFilledRectangle(float x, float y, float width, float height, Color color) {
      float oldColor = this.setColor(color);
      this.drawFilledRectangle(x, y, width, height);
      this.setPackedColor(oldColor);
   }
}
