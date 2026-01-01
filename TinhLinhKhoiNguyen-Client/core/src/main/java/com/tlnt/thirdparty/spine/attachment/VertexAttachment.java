package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.Bone;
import com.tlnt.thirdparty.spine.Slot;

public abstract class VertexAttachment extends Attachment {
   public static int color;
   public final int closed = nextId();
   @Null
   public Attachment constantSpeed = this;
   @Null
   public int[] bones;
   public float[] vertices;
   public int worldVerticesLength;

   public VertexAttachment(String name) {
      super(name);
   }

   public void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset, int stride) {
      count = offset + (count >> 1) * stride;
      FloatArray deformArray = slot.getDeform();
      float[] vertices = this.vertices;
      int[] bones = this.bones;
      if (this.bones == null) {
         if (deformArray.size > 0) {
            vertices = deformArray.items;
         }

         Bone bone;
         float x = (bone = slot.getBone()).getWorldX();
         float y = bone.getWorldY();
         float a = bone.getA();
         float b = bone.getB();
         float c = bone.getC();
         float d = bone.getD();
         start = start;

         for (int w = offset; w < count; w += stride) {
            float vx = vertices[start];
            float vy = vertices[start + 1];
            worldVertices[w] = vx * a + vy * b + x;
            worldVertices[w + 1] = vx * c + vy * d + y;
            start += 2;
         }
      } else {
         int v = 0;
         int skip = 0;

         for (int i = 0; i < start; i += 2) {
            int n = bones[v];
            v += n + 1;
            skip += n;
         }

         Object[] skeletonBones = slot.getSkeleton().getBones().items;
         if (deformArray.size == 0) {
            int w = offset;

            for (int b = skip * 3; w < count; w += stride) {
               float wx = 0.0F;
               float wy = 0.0F;

               for (int n = (start = bones[v++]) + v; v < n; b += 3) {
                  Bone bone = (Bone)skeletonBones[bones[v]];
                  float vx = vertices[b];
                  float vy = vertices[b + 1];
                  float weight = vertices[b + 2];
                  wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                  wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                  v++;
               }

               worldVertices[w] = wx;
               worldVertices[w + 1] = wy;
            }
         } else {
            float[] deform = deformArray.items;
            int w = offset;
            int b = skip * 3;

            for (int f = skip << 1; w < count; w += stride) {
               float wx = 0.0F;
               float wy = 0.0F;

               int n;
               for (int nn = (n = bones[v++]) + v; v < nn; f += 2) {
                  Bone bone = (Bone)skeletonBones[bones[v]];
                  float vx = vertices[b] + deform[f];
                  float vy = vertices[b + 1] + deform[f + 1];
                  float weight = vertices[b + 2];
                  wx += (vx * bone.getA() + vy * bone.getB() + bone.getWorldX()) * weight;
                  wy += (vx * bone.getC() + vy * bone.getD() + bone.getWorldY()) * weight;
                  v++;
                  b += 3;
               }

               worldVertices[w] = wx;
               worldVertices[w + 1] = wy;
            }
         }
      }
   }

   @Null
   public final int[] getBones() {
      return this.bones;
   }

   public final void setBones(@Null int[] bones) {
      this.bones = bones;
   }

   public final float[] getVertices() {
      return this.vertices;
   }

   public final void setVertices(float[] vertices) {
      this.vertices = vertices;
   }

   public final int getWorldVerticesLength() {
      return this.worldVerticesLength;
   }

   public final void setWorldVerticesLength(int worldVerticesLength) {
      this.worldVerticesLength = worldVerticesLength;
   }

   @Null
   public final Attachment getTimelineAttachment() {
      return this.constantSpeed;
   }

   public final void setTimelineAttachment(VertexAttachment timelineAttachment) {
      this.constantSpeed = timelineAttachment;
   }

   public final int getId() {
      return this.closed;
   }

   public static synchronized int nextId() {
      int id = color;
      color += 1;
      return id;
   }
}
