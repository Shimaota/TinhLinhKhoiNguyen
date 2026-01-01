package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public final class PolygonBatchManager extends BatchManager {
   public short[] batch;
   public int cacheDraws = 0;

   public PolygonBatchManager(PolygonBatch polygonBatch, TextureRegion region) {
      super(polygonBatch, region);
      int capacity = super.verts.length;
      int triangleArraySize = capacity / 5 * 3;
      this.batch = new short[triangleArraySize];
   }

   @Override
   public final void pushQuad() {
      short i1 = (short)super.vertexCount;
      short i2 = (short)(super.vertexCount + 1);
      this.pushTriangleIndices(i1, i2, (short)(super.vertexCount + 2));
      i1 = (short)super.vertexCount;
      i2 = (short)(super.vertexCount + 2);
      this.pushTriangleIndices(i1, i2, (short)(super.vertexCount + 3));
      super.pushQuad();
   }

   @Override
   public final void pushTriangle() {
      short i1 = (short)super.vertexCount;
      short i2 = (short)(super.vertexCount + 1);
      this.pushTriangleIndices(i1, i2, (short)(super.vertexCount + 2));
      super.vertexCount += 3;
   }

   public final void pushTriangleIndices(short t1, short t2, short t3) {
      int triangleOffset = 3 * this.cacheDraws;
      this.batch[triangleOffset] = t1;
      this.batch[triangleOffset + 1] = t2;
      this.batch[triangleOffset + 2] = t3;
      this.cacheDraws += 1;
   }

   @Override
   public final void ensureSpaceForTriangle() {
      this.ensureSpace(3);
   }

   @Override
   public final void pushToBatch() {
      if (super.vertexCount != 0) {
         PolygonBatch polygonBatch = (PolygonBatch)super.getBatch();
         Texture texture = super.textureRegion.getTexture();
         polygonBatch.draw(
            texture,
            super.verts,
            0,
            this.getVerticesArrayIndex(),
            this.batch,
            0,
            3 * this.cacheDraws
         );
         super.vertexCount = 0;
         this.cacheDraws = 0;
      }
   }

   @Override
   public final void increaseCacheSize(int minSize) {
      super.increaseCacheSize(minSize);
      int capacity = super.verts.length;
      capacity /= 5;
      minSize = capacity * 3;
      this.batch = new short[minSize];
   }
}
