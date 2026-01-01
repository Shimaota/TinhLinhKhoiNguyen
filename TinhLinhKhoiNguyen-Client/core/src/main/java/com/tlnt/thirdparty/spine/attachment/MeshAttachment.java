package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.Slot;

public final class MeshAttachment extends VertexAttachment implements HasTextureRegion {
   public TextureRegion color;
   public float[] closed;
   public float[] constantSpeed;
   public short[] triangles;
   public final Color color2 = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public int hullLength;
   @Null
   public MeshAttachment parentMesh;
   @Null
   public Sequence sequence;
   @Null
   public short[] edges;
   public float width;
   public float height;

   public MeshAttachment(String name) {
      super(name);
   }

   @Override
   public final void setRegion(TextureRegion region) {
      if (region == null) {
         throw new IllegalArgumentException("region cannot be null.");
      } else {
         this.color = region;
      }
   }

   @Null
   @Override
   public final TextureRegion getRegion() {
      return this.color;
   }

   @Override
   public final void updateRegion() {
      float[] regionUVs = this.closed;
      if (this.constantSpeed == null || this.constantSpeed.length != regionUVs.length) {
         this.constantSpeed = new float[regionUVs.length];
      }

      float[] uvs = this.constantSpeed;
      int n = this.constantSpeed.length;
      float u;
      float v;
      float width;
      float height;
      if (this.color instanceof TextureAtlas.AtlasRegion) {
         u = this.color.getU();
         v = this.color.getV();
         TextureAtlas.AtlasRegion region;
         width = (float)(region = (TextureAtlas.AtlasRegion)this.color).getTexture().getWidth();
         height = (float)region.getTexture().getHeight();
         switch (region.degrees) {
            case 90:
               u -= ((float)region.originalHeight - region.offsetY - (float)region.packedWidth) / width;
               v -= ((float)region.originalWidth - region.offsetX - (float)region.packedHeight) / height;
               width = (float)region.originalHeight / width;
               height = (float)region.originalWidth / height;

               for (int i = 0; i < n; i += 2) {
                  uvs[i] = u + regionUVs[i + 1] * width;
                  uvs[i + 1] = v + (1.0F - regionUVs[i]) * height;
               }

               return;
            case 180:
               u -= ((float)region.originalWidth - region.offsetX - (float)region.packedWidth) / width;
               v -= region.offsetY / height;
               width = (float)region.originalWidth / width;
               height = (float)region.originalHeight / height;

               for (int i = 0; i < n; i += 2) {
                  uvs[i] = u + (1.0F - regionUVs[i]) * width;
                  uvs[i + 1] = v + (1.0F - regionUVs[i + 1]) * height;
               }

               return;
            case 270:
               u -= region.offsetY / width;
               v -= region.offsetX / height;
               width = (float)region.originalHeight / width;
               height = (float)region.originalWidth / height;

               for (int i = 0; i < n; i += 2) {
                  uvs[i] = u + (1.0F - regionUVs[i + 1]) * width;
                  uvs[i + 1] = v + regionUVs[i] * height;
               }

               return;
            default:
               u -= region.offsetX / width;
               v -= ((float)region.originalHeight - region.offsetY - (float)region.packedHeight) / height;
               width = (float)region.originalWidth / width;
               height = (float)region.originalHeight / height;
         }
      } else if (this.color == null) {
         v = 0.0F;
         u = 0.0F;
         height = 1.0F;
         width = 1.0F;
      } else {
         u = this.color.getU();
         v = this.color.getV();
         width = this.color.getU2() - u;
         height = this.color.getV2() - v;
      }

      for (int i = 0; i < n; i += 2) {
         uvs[i] = u + regionUVs[i] * width;
         uvs[i + 1] = v + regionUVs[i + 1] * height;
      }
   }

   @Override
   public final void computeWorldVertices(Slot slot, int start, int count, float[] worldVertices, int offset, int stride) {
      if (this.sequence != null) {
         this.sequence.apply(slot, this);
      }

      super.computeWorldVertices(slot, start, count, worldVertices, offset, stride);
   }

   public final short[] getTriangles() {
      return this.triangles;
   }

   public final void setTriangles(short[] triangles) {
      this.triangles = triangles;
   }

   public final void setRegionUVs(float[] regionUVs) {
      this.closed = regionUVs;
   }

   public final float[] getUVs() {
      return this.constantSpeed;
   }

   public final Color getColor() {
      return this.color2;
   }

   public final void setHullLength(int hullLength) {
      this.hullLength = hullLength;
   }

   public final void setEdges(short[] edges) {
      this.edges = edges;
   }

   public final void setWidth(float width) {
      this.width = width;
   }

   public final void setHeight(float height) {
      this.height = height;
   }

   @Null
   @Override
   public final Sequence getSequence() {
      return this.sequence;
   }

   public final void setSequence(@Null Sequence sequence) {
      this.sequence = sequence;
   }

   public final void setParentMesh(@Null MeshAttachment parentMesh) {
      this.parentMesh = parentMesh;
      if (parentMesh != null) {
         super.bones = parentMesh.bones;
         super.vertices = parentMesh.vertices;
         this.closed = parentMesh.closed;
         this.triangles = parentMesh.triangles;
         this.hullLength = parentMesh.hullLength;
         super.worldVerticesLength = parentMesh.worldVerticesLength;
         this.edges = parentMesh.edges;
         this.width = parentMesh.width;
         this.height = parentMesh.height;
      }
   }

}
