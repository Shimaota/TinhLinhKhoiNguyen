package com.tlnt.thirdparty.spine.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ShortArray;

public final class Triangulator {
   public final Array<FloatArray> convexPolygons;
   public final Array<ShortArray> convexPolygonsIndices;
   public final ShortArray indicesArray;
   public final BooleanArray isConcaveArray;
   public final ShortArray triangles;
   public final Pool<FloatArray> polygonPool;
   public final Pool<ShortArray> polygonIndicesPool;

   public Triangulator() {
      this.convexPolygons = new Array<>(false, 16);
      this.convexPolygonsIndices = new Array<>(false, 16);
      this.indicesArray = new ShortArray();
      this.isConcaveArray = new BooleanArray();
      this.triangles = new ShortArray();
      this.polygonPool = new PolygonPool(this);
      this.polygonIndicesPool = new PolygonIndicesPool(this);
   }

   public final ShortArray triangulate(FloatArray verticesArray) {
      float[] vertices = verticesArray.items;
      int vertexCount = verticesArray.size >> 1;
      ShortArray indicesArray = this.indicesArray;
      this.indicesArray.clear();
      short[] indices = indicesArray.setSize(vertexCount);

      for (int k = 0; k < vertexCount; k = (short)(k + 1)) {
         indices[k] = (short)k;
      }

      BooleanArray isConcaveArray = this.isConcaveArray;
      boolean[] isConcave = this.isConcaveArray.setSize(vertexCount);

      for (int j = 0; j < vertexCount; j++) {
         isConcave[j] = isConcave(j, vertexCount, vertices, indices);
      }

      ShortArray triangles = this.triangles;
      this.triangles.clear();
      triangles.ensureCapacity(Math.max(0, vertexCount - 2) << 2);

      while (true) {
         if (vertexCount <= 3) {
            if (vertexCount == 3) {
               triangles.add(indices[2]);
               triangles.add(indices[0]);
               triangles.add(indices[1]);
            }

            return triangles;
         }

         int previous = vertexCount - 1;
         int i = 0;
         int next = 1;

         label60:
         while (true) {
            if (!isConcave[i]) {
               short vertexIndex = indices[previous];
               int p1 = vertexIndex << 1;
               vertexIndex = indices[i];
               int p2 = vertexIndex << 1;
               int p3 = indices[next] << 1;
               float p1x = vertices[p1];
               float p1y = vertices[p1 + 1];
               float p2x = vertices[p2];
               float p2y = vertices[p2 + 1];
               float p3x = vertices[p3];
               float p3y = vertices[p3 + 1];
               int ii = (next + 1) % vertexCount;

               while (true) {
                  if (ii == previous) {
                     break label60;
                  }

                  if (isConcave[ii]) {
                     vertexIndex = indices[ii];
                     int v = vertexIndex << 1;
                     float vx = vertices[v];
                     float vy = vertices[v + 1];
                     if (positiveArea(p3x, p3y, p1x, p1y, vx, vy)
                        && positiveArea(p1x, p1y, p2x, p2y, vx, vy)
                        && positiveArea(p2x, p2y, p3x, p3y, vx, vy)) {
                        break;
                     }
                  }

                  ii = (ii + 1) % vertexCount;
               }
            }

            if (next == 0) {
               while (true) {
                  if (!isConcave[i] || --i <= 0) {
                     break label60;
                  }
               }
            }

            previous = i;
            i = next;
            next = (next + 1) % vertexCount;
         }

         int wrapIndex = vertexCount + i;
         triangles.add(indices[(wrapIndex - 1) % vertexCount]);
         triangles.add(indices[i]);
         triangles.add(indices[(i + 1) % vertexCount]);
         indicesArray.removeIndex(i);
         isConcaveArray.removeIndex(i);
         int tmp = --vertexCount + i;
         int previousIndex = (tmp - 1) % vertexCount;
         if (i == vertexCount) {
            tmp = 0;
         } else {
            tmp = i;
         }

         int nextIndex = tmp;
         isConcave[previousIndex] = isConcave(previousIndex, vertexCount, vertices, indices);
         isConcave[nextIndex] = isConcave(nextIndex, vertexCount, vertices, indices);
      }
   }

   public final Array<FloatArray> decompose(FloatArray verticesArray, ShortArray triangles) {
      float[] vertices = verticesArray.items;
      Array convexPolygons = this.convexPolygons;
      this.polygonPool.freeAll(convexPolygons);
      convexPolygons.clear();
      Array convexPolygonsIndices = this.convexPolygonsIndices;
      this.polygonIndicesPool.freeAll(convexPolygonsIndices);
      convexPolygonsIndices.clear();
      ShortArray polygonIndices;
      (polygonIndices = this.polygonIndicesPool.obtain()).clear();
      FloatArray polygon;
      (polygon = this.polygonPool.obtain()).clear();
      int fanBaseIndex = -1;
      int lastWinding = 0;
      short[] trianglesItems = triangles.items;
      int i = 0;

      for (int n = triangles.size; i < n; i += 3) {
         int t1 = trianglesItems[i] << 1;
         short triangleIndex = trianglesItems[i + 1];
         int t2 = triangleIndex << 1;
         triangleIndex = trianglesItems[i + 2];
         int t3 = triangleIndex << 1;
         float x1 = vertices[t1];
         float y1 = vertices[t1 + 1];
         float x2 = vertices[t2];
         float y2 = vertices[t2 + 1];
         float x3 = vertices[t3];
         float y3 = vertices[t3 + 1];
         int merged = 0;
         if (fanBaseIndex == t1) {
            int o = polygon.size - 4;
            float[] p = polygon.items;
            float fanPrevPrevX = polygon.items[o];
            float fanPrevPrevY = p[o + 1];
            float fanPx = p[o + 2];
            int fanWinding1 = winding(fanPrevPrevX, fanPrevPrevY, fanPx, p[o + 3], x3, y3);
            fanPx = p[0];
            float fanPy = p[1];
            int fanWinding2 = winding(x3, y3, fanPx, fanPy, p[2], p[3]);
            if (fanWinding1 == lastWinding && fanWinding2 == lastWinding) {
               polygon.add(x3);
               polygon.add(y3);
               polygonIndices.add(t3);
               merged = 1;
            }
         }

         if (merged == 0) {
            if (polygon.size > 0) {
               convexPolygons.add(polygon);
               convexPolygonsIndices.add(polygonIndices);
               polygon = this.polygonPool.obtain();
               polygonIndices = this.polygonIndicesPool.obtain();
            }

            polygon.clear();
            polygon.add(x1);
            polygon.add(y1);
            polygon.add(x2);
            polygon.add(y2);
            polygon.add(x3);
            polygon.add(y3);
            polygonIndices.clear();
            polygonIndices.add(t1);
            polygonIndices.add(t2);
            polygonIndices.add(t3);
            lastWinding = winding(x1, y1, x2, y2, x3, y3);
            fanBaseIndex = t1;
         }
      }

      if (polygon.size > 0) {
         convexPolygons.add(polygon);
         convexPolygonsIndices.add(polygonIndices);
      }

      Object[] convexPolygonsIndicesItems = convexPolygonsIndices.items;
      Object[] convexPolygonsItems = convexPolygons.items;
      int j = 0;

      for (int polygonsCount = convexPolygons.size; j < polygonsCount; j++) {
         if ((polygonIndices = (ShortArray)convexPolygonsIndicesItems[j]).size != 0) {
            short firstIndex = polygonIndices.first();
            short lastIndex = polygonIndices.get(polygonIndices.size - 1);
            int lastOffset = (polygon = (FloatArray)convexPolygonsItems[j]).size - 4;
            float[] polygonItems = polygon.items;
            float prevPrevX = polygon.items[lastOffset];
            float prevPrevY = polygonItems[lastOffset + 1];
            float prevX = polygonItems[lastOffset + 2];
            float prevY = polygonItems[lastOffset + 3];
            float firstX = polygonItems[0];
            float firstY = polygonItems[1];
            float secondX = polygonItems[2];
            float secondY = polygonItems[3];
            int winding = winding(prevPrevX, prevPrevY, prevX, prevY, firstX, firstY);

            for (int ii = 0; ii < polygonsCount; ii++) {
               if (ii != j) {
                  ShortArray otherIndices;
                  if ((otherIndices = (ShortArray)convexPolygonsIndicesItems[ii]).size == 3) {
                     short otherFirstIndex = otherIndices.first();
                     int otherSecondIndex = otherIndices.get(1);
                     short otherLastIndex = otherIndices.get(2);
                     FloatArray otherPoly;
                     float otherX = (otherPoly = (FloatArray)convexPolygonsItems[ii]).get(otherPoly.size - 2);
                     float otherY = otherPoly.get(otherPoly.size - 1);
                     if (otherFirstIndex == firstIndex && otherSecondIndex == lastIndex) {
                        int winding1 = winding(prevPrevX, prevPrevY, prevX, prevY, otherX, otherY);
                        otherSecondIndex = winding(otherX, otherY, firstX, firstY, secondX, secondY);
                        if (winding1 == winding && otherSecondIndex == winding) {
                           otherPoly.clear();
                           otherIndices.clear();
                           polygon.add(otherX);
                           polygon.add(otherY);
                           polygonIndices.add((int)otherLastIndex);
                           prevPrevX = prevX;
                           prevPrevY = prevY;
                           prevX = otherX;
                           prevY = otherY;
                           ii = 0;
                        }
                     }
                  }
               }
            }
         }
      }

      for (int k = convexPolygons.size - 1; k >= 0; k--) {
         if ((polygon = (FloatArray)convexPolygonsItems[k]).size == 0) {
            convexPolygons.removeIndex(k);
            this.polygonPool.free(polygon);
            polygonIndices = (ShortArray)convexPolygonsIndices.removeIndex(k);
            this.polygonIndicesPool.free(polygonIndices);
         }
      }

      return convexPolygons;
   }

   public static boolean isConcave(int index, int vertexCount, float[] vertices, short[] indices) {
      int wrapIndex = vertexCount + index;
      int previous = indices[(wrapIndex - 1) % vertexCount] << 1;
      short vertexIndex = indices[index];
      int current = vertexIndex << 1;
      vertexIndex = indices[(index + 1) % vertexCount];
      index = vertexIndex << 1;
      float previousX = vertices[previous];
      float previousY = vertices[previous + 1];
      float currentX = vertices[current];
      float currentY = vertices[current + 1];
      float nextX = vertices[index];
      if (!positiveArea(previousX, previousY, currentX, currentY, nextX, vertices[index + 1])) {
         return true;
      } else {
         return false;
      }
   }

   public static boolean positiveArea(float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
      return ((p1x * (p3y - p2y) + p2x * (p1y - p3y) + p3x * (p2y - p1y) >= 0.0F ? 1 : 0) != 0);
   }

   public static int winding(float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
      p2x -= p1x;
      p2y -= p1y;
      if (p3x * p2y - p3y * p2x + p2x * p1y - p1x * p2y >= 0.0F) {
         return 1;
      } else {
         return -1;
      }
   }
}
