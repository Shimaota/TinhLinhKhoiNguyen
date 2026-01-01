package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.Bone;
import com.tlnt.thirdparty.spine.Slot;

public final class RegionAttachment extends Attachment implements HasTextureRegion {
   public TextureRegion bones;
   public float x;
   public float y;
   public float scaleX = 1.0F;
   public float scaleY = 1.0F;
   public float rotation;
   public float width;
   public float height;
   public final float[] uvs;
   public final float[] offset;
   public final Color color;
   @Null
   public Sequence sequence;

   public RegionAttachment(String name) {
      super(name);
      this.uvs = new float[8];
      this.offset = new float[8];
      this.color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   public final void updateRegion() {
      if (this.bones == null) {
         this.uvs[0] = 0.0F;
         this.uvs[1] = 0.0F;
         this.uvs[2] = 0.0F;
         this.uvs[3] = 1.0F;
         this.uvs[4] = 1.0F;
         this.uvs[5] = 1.0F;
         this.uvs[6] = 1.0F;
         this.uvs[7] = 0.0F;
      } else {
         float widthThenSin = this.width;
         float heightThenCos = this.height;
         float localX2 = widthThenSin / 2.0F;
         float localY2 = heightThenCos / 2.0F;
         float localX = -localX2;
         float localY = -localY2;
         int rotated = 0;
         if (this.bones instanceof TextureAtlas.AtlasRegion) {
            TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion)this.bones;
            localX += region.offsetX / (float)region.originalWidth * widthThenSin;
            localY += region.offsetY / (float)region.originalHeight * heightThenCos;
            if (region.degrees == 90) {
               rotated = 1;
               localX2 -= ((float)region.originalWidth - region.offsetX - (float)region.packedHeight) / (float)region.originalWidth * widthThenSin;
               localY2 -= ((float)region.originalHeight - region.offsetY - (float)region.packedWidth) / (float)region.originalHeight * heightThenCos;
            } else {
               localX2 -= ((float)region.originalWidth - region.offsetX - (float)region.packedWidth) / (float)region.originalWidth * widthThenSin;
               localY2 -= ((float)region.originalHeight - region.offsetY - (float)region.packedHeight) / (float)region.originalHeight * heightThenCos;
            }
         }

         float scaleXThenX = this.scaleX;
         widthThenSin = this.scaleY;
         localX *= scaleXThenX;
         localY *= widthThenSin;
         localX2 *= scaleXThenX;
         localY2 *= widthThenSin;
         heightThenCos = (float)Math.cos((double)(widthThenSin = this.rotation * (float) (Math.PI / 180.0)));
         widthThenSin = (float)Math.sin((double)widthThenSin);
         scaleXThenX = this.x;
         float y = this.y;
         float localXCos = localX * heightThenCos + scaleXThenX;
         localX *= widthThenSin;
         float localYCos = localY * heightThenCos + y;
         localY *= widthThenSin;
         scaleXThenX = localX2 * heightThenCos + scaleXThenX;
         localX2 *= widthThenSin;
         heightThenCos = localY2 * heightThenCos + y;
         widthThenSin = localY2 * widthThenSin;
         float[] offset = this.offset;
         this.offset[0] = localXCos - localY;
         offset[1] = localYCos + localX;
         offset[2] = localXCos - widthThenSin;
         offset[3] = heightThenCos + localX;
         offset[4] = scaleXThenX - widthThenSin;
         offset[5] = heightThenCos + localX2;
         offset[6] = scaleXThenX - localY;
         offset[7] = localYCos + localX2;
         float[] uvs = this.uvs;
         if (rotated != 0) {
            uvs[0] = this.bones.getU2();
            uvs[1] = this.bones.getV();
            uvs[2] = this.bones.getU2();
            uvs[3] = this.bones.getV2();
            uvs[4] = this.bones.getU();
            uvs[5] = this.bones.getV2();
            uvs[6] = this.bones.getU();
            uvs[7] = this.bones.getV();
         } else {
            uvs[0] = this.bones.getU2();
            uvs[1] = this.bones.getV2();
            uvs[2] = this.bones.getU();
            uvs[3] = this.bones.getV2();
            uvs[4] = this.bones.getU();
            uvs[5] = this.bones.getV();
            uvs[6] = this.bones.getU2();
            uvs[7] = this.bones.getV();
         }
      }
   }

   @Override
   public final void setRegion(TextureRegion region) {
      if (region == null) {
         throw new IllegalArgumentException("region cannot be null.");
      } else {
         this.bones = region;
      }
   }

   @Null
   @Override
   public final TextureRegion getRegion() {
      return this.bones;
   }

   public final void computeWorldVertices(Slot slot, float[] worldVertices, int offset, int stride) {
      if (this.sequence != null) {
         this.sequence.apply(slot, this);
      }

      float[] vertexOffset = this.offset;
      Bone bone;
      float x = (bone = slot.getBone()).getWorldX();
      float y = bone.getWorldY();
      float a = bone.getA();
      float b = bone.getB();
      float c = bone.getC();
      float d = bone.getD();
      float offsetX = vertexOffset[6];
      float offsetY = vertexOffset[7];
      worldVertices[0] = offsetX * a + offsetY * b + x;
      worldVertices[1] = offsetX * c + offsetY * d + y;
      offset = stride + 0;
      offsetX = vertexOffset[0];
      offsetY = vertexOffset[1];
      worldVertices[offset] = offsetX * a + offsetY * b + x;
      worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
      offset += stride;
      offsetX = vertexOffset[2];
      offsetY = vertexOffset[3];
      worldVertices[offset] = offsetX * a + offsetY * b + x;
      worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
      offset += stride;
      offsetX = vertexOffset[4];
      offsetY = vertexOffset[5];
      worldVertices[offset] = offsetX * a + offsetY * b + x;
      worldVertices[offset + 1] = offsetX * c + offsetY * d + y;
   }

   public final float[] getUVs() {
      return this.uvs;
   }

   public final void setX(float x) {
      this.x = x;
   }

   public final void setY(float y) {
      this.y = y;
   }

   public final void setScaleX(float scaleX) {
      this.scaleX = scaleX;
   }

   public final void setScaleY(float scaleY) {
      this.scaleY = scaleY;
   }

   public final void setRotation(float rotation) {
      this.rotation = rotation;
   }

   public final void setWidth(float width) {
      this.width = width;
   }

   public final void setHeight(float height) {
      this.height = height;
   }

   public final Color getColor() {
      return this.color;
   }

   @Null
   @Override
   public final Sequence getSequence() {
      return this.sequence;
   }

   public final void setSequence(@Null Sequence sequence) {
      this.sequence = sequence;
   }

}
