package com.tlnt.thirdparty.spine.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.tlnt.thirdparty.spine.Slot;
import com.tlnt.thirdparty.spine.attachment.ClippingAttachment;

public final class SkeletonClipping {
   public final Triangulator triangulator = new Triangulator();
   public final FloatArray clippingPolygon = new FloatArray();
   public final FloatArray clipOutput;
   public final FloatArray clippedVertices;
   public final FloatArray clippedUVs;
   public final ShortArray clippedTriangles;
   public final FloatArray scratch;
   public ClippingAttachment clipAttachment;
   public Array<FloatArray> clippingPolygons;

   public SkeletonClipping() {
      this.clipOutput = new FloatArray(128);
      this.clippedVertices = new FloatArray(128);
      this.clippedUVs = new FloatArray(128);
      this.clippedTriangles = new ShortArray(128);
      this.scratch = new FloatArray();
   }

   public final void clipStart(Slot slot, ClippingAttachment clip) {
      if (this.clipAttachment == null) {
         int n;
         int worldVerticesLength = n = clip.getWorldVerticesLength();
         if (worldVerticesLength >= 6) {
            this.clipAttachment = clip;
            float[] vertices = this.clippingPolygon.setSize(n);
            clip.computeWorldVertices(slot, 0, n, vertices, 0, 2);
            makeClockwise(this.clippingPolygon);
            ShortArray triangles = this.triangulator.triangulate(this.clippingPolygon);
            this.clippingPolygons = this.triangulator.decompose(this.clippingPolygon, triangles);
            Array.ArrayIterator polygonIterator = this.clippingPolygons.iterator();

            while (polygonIterator.hasNext()) {
               FloatArray polygon;
               makeClockwise(polygon = (FloatArray)polygonIterator.next());
               polygon.add(polygon.items[0]);
               polygon.add(polygon.items[1]);
            }
         }
      }
   }

   public final void clipEndWithSlot(Slot slot) {
      if (this.clipAttachment != null && this.clipAttachment.getEndSlot() == slot.getData()) {
         this.clipEnd();
      }
   }

   public final void clipEnd() {
      if (this.clipAttachment != null) {
         this.clipAttachment = null;
         this.clippingPolygons = null;
         this.clippedVertices.clear();
         this.clippedUVs.clear();
         this.clippedTriangles.clear();
         this.clippingPolygon.clear();
      }
   }

   public final boolean isClipping() {
      if (this.clipAttachment != null) {
         return true;
      } else {
         return false;
      }
   }

   public final void clipTriangles(float[] vertices, short[] triangles, int trianglesLength, float[] uvs, float light, float dark, boolean twoColor) {
      FloatArray clipOutput = this.clipOutput;
      FloatArray clippedVertices = this.clippedVertices;
      ShortArray clippedTriangles = this.clippedTriangles;
      Object[] polygons = this.clippingPolygons.items;
      int polygonsCount = this.clippingPolygons.size;
      int vertexSize;
      if (twoColor) {
         vertexSize = 6;
      } else {
         vertexSize = 5;
      }

      int stride = vertexSize;
      int index = 0;
      clippedVertices.clear();
      this.clippedUVs.clear();
      clippedTriangles.clear();

      for (int i = 0; i < trianglesLength; i += 3) {
         int vertexOffset = triangles[i] << 1;
         float x1 = vertices[vertexOffset];
         float y1 = vertices[vertexOffset + 1];
         float u1 = uvs[vertexOffset];
         float v1 = uvs[vertexOffset + 1];
         vertexOffset = triangles[i + 1] << 1;
         float x2 = vertices[vertexOffset];
         float y2 = vertices[vertexOffset + 1];
         float u2 = uvs[vertexOffset];
         float v2 = uvs[vertexOffset + 1];
         int tmp = triangles[i + 2];
         vertexOffset = tmp << 1;
         float x3 = vertices[vertexOffset];
         float y3 = vertices[vertexOffset + 1];
         float u3 = uvs[vertexOffset];
         float v3 = uvs[vertexOffset + 1];

         for (int p = 0; p < polygonsCount; p++) {
            int s = clippedVertices.size;
            FloatArray polygon = (FloatArray)polygons[p];
            FloatArray output = clipOutput;
            FloatArray clippingArea = polygon;
            int clipped = 0;
            tmp = clippingArea.size % 4;
            FloatArray input;
            if (tmp >= 2) {
               input = clipOutput;
               output = this.scratch;
            } else {
               input = this.scratch;
            }

            input.clear();
            input.add(x1);
            input.add(y1);
            input.add(x2);
            input.add(y2);
            input.add(x3);
            input.add(y3);
            input.add(x1);
            input.add(y1);
            output.clear();
            int limit = clippingArea.size - 4;
            float[] clippingVertices = clippingArea.items;
            int edgeIndex = 0;

            while (true) {
               float edgeX = clippingVertices[edgeIndex];
               float edgeY = clippingVertices[edgeIndex + 1];
               float deltaX = edgeX - clippingVertices[edgeIndex + 2];
               float deltaY = edgeY - clippingVertices[edgeIndex + 3];
               int outputStart = output.size;
               float[] inputVertices = input.items;
               int ii = 0;
               int inputVerticesLength = input.size - 2;

               while (ii < inputVerticesLength) {
                  float inputX = inputVertices[ii];
                  float inputY = inputVertices[ii + 1];
                  ii += 2;
                  float inputX2 = inputVertices[ii];
                  float inputY2 = inputVertices[ii + 1];
                  if (deltaY * (edgeX - inputX2) > deltaX * (edgeY - inputY2)) {
                     tmp = 1;
                  } else {
                     tmp = 0;
                  }

                  int side2 = tmp;
                  float s1;
                  if ((s1 = deltaY * (edgeX - inputX) - deltaX * (edgeY - inputY)) > 0.0F) {
                     if (side2 != 0) {
                        output.add(inputX2);
                        output.add(inputY2);
                        continue;
                     }

                     float ix = inputX2 - inputX;
                     float iy = inputY2 - inputY;
                     float t;
                     if (!((t = s1 / (ix * deltaY - iy * deltaX)) >= 0.0F) || !(t <= 1.0F)) {
                        output.add(inputX2);
                        output.add(inputY2);
                        continue;
                     }

                     output.add(inputX + ix * t);
                     output.add(inputY + iy * t);
                  } else if (side2 != 0) {
                     float ix2 = inputX2 - inputX;
                     float iy2 = inputY2 - inputY;
                     float t2;
                     if (!((t2 = s1 / (ix2 * deltaY - iy2 * deltaX)) >= 0.0F) || !(t2 <= 1.0F)) {
                        output.add(inputX2);
                        output.add(inputY2);
                        continue;
                     }

                     output.add(inputX + ix2 * t2);
                     output.add(inputY + iy2 * t2);
                     output.add(inputX2);
                     output.add(inputY2);
                  }

                  clipped = 1;
               }

               if (outputStart == output.size) {
                  clipOutput.clear();
                  tmp = 1;
                  break;
               }

               output.add(output.items[0]);
               output.add(output.items[1]);
               if (edgeIndex == limit) {
                  if (clipOutput != output) {
                     clipOutput.clear();
                     clipOutput.addAll(output.items, 0, output.size - 2);
                  } else {
                     clipOutput.setSize(clipOutput.size - 2);
                  }

                  tmp = clipped;
                  break;
               }

               FloatArray temp = output;
               output = input;
               input.clear();
               input = temp;
               edgeIndex += 2;
            }

            if (tmp == 0) {
               float[] unclippedVerticesItems;
               (unclippedVerticesItems = clippedVertices.setSize(s + 3 * stride))[s] = x1;
               unclippedVerticesItems[s + 1] = y1;
               unclippedVerticesItems[s + 2] = light;
               if (!twoColor) {
                  unclippedVerticesItems[s + 3] = u1;
                  unclippedVerticesItems[s + 4] = v1;
                  unclippedVerticesItems[s + 5] = x2;
                  unclippedVerticesItems[s + 6] = y2;
                  unclippedVerticesItems[s + 7] = light;
                  unclippedVerticesItems[s + 8] = u2;
                  unclippedVerticesItems[s + 9] = v2;
                  unclippedVerticesItems[s + 10] = x3;
                  unclippedVerticesItems[s + 11] = y3;
                  unclippedVerticesItems[s + 12] = light;
                  unclippedVerticesItems[s + 13] = u3;
                  unclippedVerticesItems[s + 14] = v3;
               } else {
                  unclippedVerticesItems[s + 3] = dark;
                  unclippedVerticesItems[s + 4] = u1;
                  unclippedVerticesItems[s + 5] = v1;
                  unclippedVerticesItems[s + 6] = x2;
                  unclippedVerticesItems[s + 7] = y2;
                  unclippedVerticesItems[s + 8] = light;
                  unclippedVerticesItems[s + 9] = dark;
                  unclippedVerticesItems[s + 10] = u2;
                  unclippedVerticesItems[s + 11] = v2;
                  unclippedVerticesItems[s + 12] = x3;
                  unclippedVerticesItems[s + 13] = y3;
                  unclippedVerticesItems[s + 14] = light;
                  unclippedVerticesItems[s + 15] = dark;
                  unclippedVerticesItems[s + 16] = u3;
                  unclippedVerticesItems[s + 17] = v3;
               }

               s = clippedTriangles.size;
               short[] unclippedTrianglesItems;
               (unclippedTrianglesItems = clippedTriangles.setSize(s + 3))[s] = (short)index;
               int sNext = s + 1;
               unclippedTrianglesItems[sNext] = (short)(index + 1);
               unclippedTrianglesItems[s + 2] = (short)(index + 2);
               index = (short)(index + 3);
               break;
            }

            limit = clipOutput.size;
            if (clipOutput.size != 0) {
               float d0 = y2 - y3;
               float d1 = x3 - x2;
               float d2 = x1 - x3;
               float d4 = y3 - y1;
               float d = 1.0F / (d0 * d2 + d1 * (y1 - y3));
               int clipOutputCount = limit >> 1;
               float[] clipOutputItems = clipOutput.items;
               float[] clippedVerticesItems = clippedVertices.setSize(s + clipOutputCount * stride);
               int clipOutputIndex = 0;

               while (clipOutputIndex < limit) {
                  float x = clipOutputItems[clipOutputIndex];
                  float y = clipOutputItems[clipOutputIndex + 1];
                  clippedVerticesItems[s] = x;
                  clippedVerticesItems[s + 1] = y;
                  clippedVerticesItems[s + 2] = light;
                  if (twoColor) {
                     clippedVerticesItems[s + 3] = dark;
                     s += 4;
                  } else {
                     s += 3;
                  }

                  float c0 = x - x3;
                  float c1 = y - y3;
                  float a = (d0 * c0 + d1 * c1) * d;
                  float b = (d4 * c0 + d2 * c1) * d;
                  float c = 1.0F - a - b;
                  clippedVerticesItems[s] = u1 * a + u2 * b + u3 * c;
                  clippedVerticesItems[s + 1] = v1 * a + v2 * b + v3 * c;
                  clipOutputIndex += 2;
                  s += 2;
               }

               s = clippedTriangles.size;
               short[] clippedTrianglesItems = clippedTriangles.setSize(s + 3 * (clipOutputCount - 2));
               clipOutputCount--;

               for (int fanIndex = 1; fanIndex < clipOutputCount; s += 3) {
                  clippedTrianglesItems[s] = (short)index;
                  clippedTrianglesItems[s + 1] = (short)(index + fanIndex);
                  clippedTrianglesItems[s + 2] = (short)(index + fanIndex + 1);
                  fanIndex++;
               }

               index = (short)(index + clipOutputCount + 1);
            }
         }
      }
   }

   public final FloatArray getClippedVertices() {
      return this.clippedVertices;
   }

   public final ShortArray getClippedTriangles() {
      return this.clippedTriangles;
   }

   public static void makeClockwise(FloatArray polygon) {
      float[] vertices = polygon.items;
      int verticesLength = polygon.size;
      float product = vertices[verticesLength - 2];
      product *= vertices[1];
      float area = product - vertices[0] * vertices[verticesLength - 1];
      int i = 0;

      for (int n = verticesLength - 3; i < n; i += 2) {
         float p1x = vertices[i];
         float p1y = vertices[i + 1];
         float p2x = vertices[i + 2];
         float p2y = vertices[i + 3];
         area += p1x * p2y - p2x * p1y;
      }

      if (!(area < 0.0F)) {
         i = 0;
         int lastX = verticesLength - 2;

         for (int half = verticesLength >> 1; i < half; i += 2) {
            area = vertices[i];
            float y = vertices[i + 1];
            int other = lastX - i;
            vertices[i] = vertices[other];
            vertices[i + 1] = vertices[other + 1];
            vertices[other] = area;
            vertices[other + 1] = y;
         }
      }
   }
}
