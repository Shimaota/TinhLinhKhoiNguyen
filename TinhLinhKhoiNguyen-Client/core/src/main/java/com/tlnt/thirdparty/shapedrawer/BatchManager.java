package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;

public class BatchManager {
   public Batch batch;
   public TextureRegion textureRegion;
   public float floatBits;
   public float[] verts;
   public int vertexCount;
   public float pixelSize = 1.0F;
   public float halfPixelSize = 0.5F * this.pixelSize;
   public float offset = 0.001F * this.pixelSize;
   public boolean cacheDraws;
   public Drawing drawing;

   public BatchManager(Batch batch, TextureRegion region) {
      this.cacheDraws = false;
      this.drawing = null;
      this.batch = batch;
      this.verts = new float[2000];
      TextureRegion oldRegion = this.textureRegion;
      this.textureRegion = region;
      this.updateRegionUV();
      Color white = Color.WHITE;
      this.setPackedColor(white.toFloatBits());
   }

   public void updateRegionUV() {
      if (this.textureRegion != null) {
         float u = 0.5F * (this.textureRegion.getU() + this.textureRegion.getU2());
         float v = 0.5F * (this.textureRegion.getV() + this.textureRegion.getV2());

         for (int i = 0; i < this.verts.length; i += 5) {
            this.verts[i + 3] = u;
            this.verts[i + 4] = v;
         }
      }
   }

   public final float setPackedColor(float floatBits) {
      float oldColor = this.floatBits;
      this.floatBits = floatBits;
      return oldColor;
   }

   public final boolean isCachingDraws() {
      return this.cacheDraws;
   }

   public final boolean startCaching() {
      boolean wasCaching = this.cacheDraws;
      this.cacheDraws = true;
      return wasCaching;
   }

   public final void endCaching() {
      this.cacheDraws = false;
      if (this.vertexCount > 0) {
         this.pushToBatch();
      }
   }

   public final float getPixelSize() {
      return this.pixelSize;
   }

   public Batch getBatch() {
      return this.batch;
   }

   public final void pushVertex() {
      this.vertexCount += 1;
   }

   public void pushQuad() {
      this.vertexCount += 4;
   }

   public void pushTriangle() {
      this.setX4(this.getX3());
      this.setY4(this.getY3());
      this.pushQuad();
   }

   public void ensureSpaceForTriangle() {
      this.ensureSpace(4);
   }

   public final void ensureSpaceForQuad() {
      this.ensureSpace(4);
   }

   public final void ensureSpace(int vertices) {
      if (vertices * 5 > this.verts.length) {
         this.increaseCacheSize(vertices * 5);
      } else {
         if (this.getVerticesRemaining() < vertices) {
            this.pushToBatch();
         }
      }
   }

   public void increaseCacheSize(int minSize) {
      this.pushToBatch();
      int newSize = this.verts.length;

      while (minSize > newSize) {
         newSize <<= 1;
      }

      this.verts = new float[newSize];
      this.updateRegionUV();
   }

   public int getVerticesRemaining() {
      return (this.verts.length - 20 * this.vertexCount) / 5;
   }

   public void pushToBatch() {
      if (this.vertexCount != 0) {
         if (this.textureRegion == null) {
            throw new IllegalStateException("The texture region is null. Please set a texture region first (e.g. in the constructor or by calling setTextureRegion(TextureRegion region))");
         } else {
            Batch batch = this.batch;
            Texture texture = this.textureRegion.getTexture();
            batch.draw(texture, this.verts, 0, 5 * this.vertexCount);
            this.vertexCount = 0;
         }
      }
   }

   public final int getVerticesArrayIndex() {
      return 5 * this.vertexCount;
   }

   public final void setX1(float x) {
      this.verts[5 * this.vertexCount] = x;
   }

   public final void setY1(float y) {
      this.verts[5 * this.vertexCount + 1] = y;
   }

   public final void setX2(float x) {
      this.verts[5 * this.vertexCount + 5] = x;
   }

   public final void setY2(float y) {
      int vertexIndex = 5 * this.vertexCount;
      this.verts[vertexIndex + 6] = y;
   }

   public final void setX3(float x) {
      int vertexIndex = 5 * this.vertexCount;
      this.verts[vertexIndex + 10] = x;
   }

   public final void setY3(float y) {
      this.verts[5 * this.vertexCount + 11] = y;
   }

   public final void setX4(float x) {
      this.verts[5 * this.vertexCount + 15] = x;
   }

   public final void setY4(float y) {
      int vertexIndex = 5 * this.vertexCount;
      this.verts[vertexIndex + 16] = y;
   }

   public final float getX3() {
      int vertexIndex = 5 * this.vertexCount;
      return this.verts[vertexIndex + 10];
   }

   public final float getY3() {
      return this.verts[5 * this.vertexCount + 11];
   }

   public final float getX4() {
      return this.verts[5 * this.vertexCount + 15];
   }

   public final float getY4() {
      int vertexIndex = 5 * this.vertexCount;
      return this.verts[vertexIndex + 16];
   }

   public final void setColor1(float c) {
      this.verts[5 * this.vertexCount + 2] = c;
   }

   public final void setColor2(float c) {
      int vertexIndex = 5 * this.vertexCount;
      this.verts[vertexIndex + 7] = c;
   }

   public final void setColor3(float c) {
      int vertexIndex = 5 * this.vertexCount;
      this.verts[vertexIndex + 12] = c;
   }

   public final void setColor4(float c) {
      int vertexIndex = 5 * this.vertexCount;
      this.verts[vertexIndex + 17] = c;
   }

   static {
      new Matrix4();
   }

}
