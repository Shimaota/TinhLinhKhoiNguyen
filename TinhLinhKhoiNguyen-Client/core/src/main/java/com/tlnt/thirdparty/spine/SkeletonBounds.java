package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.thirdparty.spine.attachment.Attachment;
import com.tlnt.thirdparty.spine.attachment.BoundingBoxAttachment;

public final class SkeletonBounds {
   public float minX;
   public float minY;
   public float maxX;
   public float maxY;
   public Array<BoundingBoxAttachment> boundingBoxes = new Array<>();
   public Array<FloatArray> polygons = new Array<>();
   public Pool<FloatArray> polygonPool = new PolygonFloatArrayPool(this);

   public final void update(Skeleton skeleton, boolean updateAabb) {
      if (skeleton == null) {
         throw new IllegalArgumentException("skeleton cannot be null.");
      } else {
         Array boundingBoxes = this.boundingBoxes;
         Array polygons = this.polygons;
         Object[] slots = skeleton.slots.items;
         int n = skeleton.slots.size;
         boundingBoxes.clear();
         this.polygonPool.freeAll(polygons);
         polygons.clear();

         for (int i = 0; i < n; i++) {
            Slot slot;
            if ((slot = (Slot)slots[i]).bone.active) {
               Attachment attachment = slot.attachment;
               if (slot.attachment instanceof BoundingBoxAttachment) {
                  BoundingBoxAttachment boundingBox = (BoundingBoxAttachment)attachment;
                  boundingBoxes.add(boundingBox);
                  FloatArray polygon = this.polygonPool.obtain();
                  polygons.add(polygon);
                  int worldVerticesLength = boundingBox.getWorldVerticesLength();
                  float[] polygonVertices = polygon.setSize(boundingBox.getWorldVerticesLength());
                  boundingBox.computeWorldVertices(slot, 0, worldVerticesLength, polygonVertices, 0, 2);
               }
            }
         }

         float minX = 2.1474836E9F;
         float minY = 2.1474836E9F;
         float maxX = -2.1474836E9F;
         float maxY = -2.1474836E9F;
         Object[] polygonItems = this.polygons.items;
         int ii = 0;

         for (int nn = this.polygons.size; ii < nn; ii++) {
            FloatArray boundsPolygon;
            float[] vertices = (boundsPolygon = (FloatArray)polygonItems[ii]).items;
            int j = 0;

            for (int vertexCount = boundsPolygon.size; j < vertexCount; j += 2) {
               float x = vertices[j];
               float y = vertices[j + 1];
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
            }
         }

         this.minX = minX;
         this.minY = minY;
         this.maxX = maxX;
         this.maxY = maxY;
      }
   }

   @Null
   public final BoundingBoxAttachment containsPoint(float x, float y) {
      Object[] polygons = this.polygons.items;
      int i = 0;

      for (int n = this.polygons.size; i < n; i++) {
         FloatArray polygonEntry = (FloatArray)polygons[i];
         float py = y;
         float px = x;
         FloatArray polygon = polygonEntry;
         if (polygonEntry == null) {
            throw new IllegalArgumentException("polygon cannot be null.");
         }

         float[] vertices = polygon.items;
         int nn;
         int prevIndex = (nn = polygon.size) - 2;
         int inside = 0;

         for (int ii = 0; ii < nn; ii += 2) {
            float vertexY = vertices[ii + 1];
            float prevY = vertices[prevIndex + 1];
            float vertexX;
            if ((vertexY < py && prevY >= py || prevY < py && vertexY >= py)
               && (vertexX = vertices[ii]) + (py - vertexY) / (prevY - vertexY) * (vertices[prevIndex] - vertexX) < px) {
               int insideFlag;
               if (inside == 0) {
                  insideFlag = 1;
               } else {
                  insideFlag = 0;
               }

               inside = insideFlag;
            }

            prevIndex = ii;
         }

         if (inside != 0) {
            return this.boundingBoxes.get(i);
         }
      }

      return null;
   }

   public final float getWidth() {
      return this.maxX - this.minX;
   }

   public final float getHeight() {
      return this.maxY - this.minY;
   }

}
