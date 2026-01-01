package com.tlnt.engine.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public final class DeformableSprite {
   public Texture texture;
   public final float[] vertices = new float[60];
   public float lastX;
   public float lastY;
   public float lastAngle;
   public float lastWidth;
   public float lastHeight;
   public float u;
   public float u2;
   public float v;
   public float v2;
   public float uRange;
   public float vRange;
   public boolean dirty = true;
   public int quadCount;
   public final float baseAngle;
   public float originOffsetX;
   public float originOffsetY;
   public float scaleX;
   public float scaleY;
   public boolean flipX;
   public boolean flipY;
   public boolean clockwise;

   public DeformableSprite(TextureRegion region) {
      this.quadCount = 0;
      this.baseAngle = 270.0F;
      this.scaleX = 1.0F;
      this.scaleY = 1.0F;
      this.clockwise = true;
      this.texture = region.getTexture();
      this.u = region.getU();
      this.v = region.getV();
      this.u2 = region.getU2();
      this.v2 = region.getV2();
      this.uRange = this.u2 - this.u;
      this.vRange = this.v2 - this.v;
      this.lastWidth = (float)region.getRegionWidth();
      this.lastHeight = (float)region.getRegionHeight();
      this.setColor(Color.WHITE);
   }

   public void setPackedColor(float packedColor) {

      for (int i = 0; i < 12; i++) {
         this.vertices[i * 5 + 2] = packedColor;
      }
   }

   public final void setColor(Color color) {
      this.setPackedColor(color.toFloatBits());
   }

   public void setVertex(float[] verts, int offset, float x, float y) {
      float texU = this.u + this.uRange * ((x - this.lastX) / this.lastWidth);
      float texV = this.v + this.vRange * (1.0F - (y - this.lastY) / this.lastHeight);
      verts[offset] = this.lastX + this.originOffsetX + (x - this.lastX - this.originOffsetX) * this.scaleX;
      verts[offset + 1] = this.lastY + this.originOffsetY + (y - this.lastY - this.originOffsetY) * this.scaleY;
      verts[offset + 3] = texU;
      verts[offset + 4] = texV;
   }

   public final void draw(Batch batch, float x, float y, float width, float height, float angle) {
      if (this.clockwise) {
         angle = 360.0F - angle;
      }

      width = -width;
      x -= width;
      height = -height;
      y -= height;
      if (width < 0.0F) {
         this.scaleX = -1.0F;
         width = -width;
      }

      if (height < 0.0F) {
         this.scaleY = -1.0F;
         height = -height;
      }

      float centerY = this.v2;
      float centerX = this.u2;
      float halfHeight = this.v;
      float halfWidth = this.u;
      if (this.dirty
         || this.lastX != x
         || this.lastY != y
         || this.lastAngle != angle
         || this.lastWidth != width
         || this.lastHeight != height
         || this.u != halfWidth
         || this.v2 != centerY
         || this.u2 != centerX
         || this.v2 != centerY) {
         this.lastX = x;
         this.lastY = y;
         this.lastWidth = width;
         this.lastHeight = height;
         this.lastAngle = angle;
         this.u = halfWidth;
         this.v = halfHeight;
         this.u2 = centerX;
         this.v2 = centerY;
         halfWidth = width * 0.5F;
         halfHeight = height * 0.5F;
         float right = x + width;
         float top = y + height;
         centerX = x + halfWidth;
         centerY = y + halfHeight;
         float cos = MathUtils.cosDeg(angle + 270.0F);
         float sin = MathUtils.sinDeg(angle + 270.0F);
         float distX = cos != 0.0F ? Math.abs(halfWidth / cos) : 1.0E8F;
         float distY = sin != 0.0F ? Math.abs(halfHeight / sin) : 1.0E8F;
         float edgeY;
         float edgeX = (edgeY = Math.min(distX, distY)) * cos;
         edgeY *= sin;
         this.setVertex(this.vertices, 5, x + halfWidth, y);
         if (cos >= 0.0F) {
            this.setVertex(this.vertices, 15, x, top);
            this.setVertex(this.vertices, 0, centerX, top);
            this.setVertex(this.vertices, 10, x, y);
            this.setVertex(this.vertices, 30, centerX, centerY);
            this.setVertex(this.vertices, 35, centerX, top);
            if (distX < distY) {
               this.setVertex(this.vertices, 20, right, top);
               this.setVertex(this.vertices, 25, right, centerY + edgeY);
               this.quadCount = 2;
            } else if (sin > 0.0F) {
               this.setVertex(this.vertices, 25, centerX + edgeX, top);
               this.setVertex(this.vertices, 20, centerX + edgeX * 0.5F, top);
               this.quadCount = 2;
            } else {
               this.setVertex(this.vertices, 20, right, top);
               this.setVertex(this.vertices, 25, right, y);
               this.setVertex(this.vertices, 55, centerX, centerY);
               this.setVertex(this.vertices, 40, right, y);
               this.setVertex(this.vertices, 50, centerX + edgeX, y);
               this.setVertex(this.vertices, 45, centerX + edgeX * 0.5F, y);
               this.quadCount = 3;
            }
         } else {
            this.setVertex(this.vertices, 0, x + halfWidth, y + halfHeight);
            if (distX < distY) {
               this.setVertex(this.vertices, 10, x, y);
               this.setVertex(this.vertices, 15, x, centerY + edgeY);
               this.quadCount = 1;
            } else if (sin < 0.0F) {
               this.setVertex(this.vertices, 15, centerX + edgeX, y);
               this.setVertex(this.vertices, 10, centerX + edgeX * 0.5F, y);
               this.quadCount = 1;
            } else {
               this.setVertex(this.vertices, 15, x, top);
               this.setVertex(this.vertices, 10, x, y);
               this.setVertex(this.vertices, 25, centerX, centerY);
               this.setVertex(this.vertices, 30, x, top);
               this.setVertex(this.vertices, 35, centerX + edgeX * 0.5F, top);
               this.setVertex(this.vertices, 20, centerX + edgeX, top);
               this.quadCount = 2;
            }
         }

         this.dirty = false;
      }

      batch.draw(this.texture, this.vertices, 0, 20 * this.quadCount);
   }
}
