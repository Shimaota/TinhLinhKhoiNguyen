package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ShortArray;
import com.tlnt.thirdparty.spine.attachment.Attachment;
import com.tlnt.thirdparty.spine.attachment.ClippingAttachment;
import com.tlnt.thirdparty.spine.attachment.MeshAttachment;
import com.tlnt.thirdparty.spine.attachment.RegionAttachment;
import com.tlnt.thirdparty.spine.attachment.SkeletonAttachment;
import com.tlnt.thirdparty.spine.utils.SkeletonClipping;
import com.tlnt.thirdparty.spine.utils.TwoColorPolygonBatch;

public final class SkeletonRenderer {
   public static final short[] QUAD_TRIANGLES;
   public boolean premultipliedAlpha;
   public boolean pmaColors;
   public final FloatArray vertices;
   public final SkeletonClipping clipper;

   public SkeletonRenderer() {
      this.vertices = new FloatArray(32);
      this.clipper = new SkeletonClipping();
   }

   public final void draw(Batch batch, Skeleton skeleton) {
      if (batch instanceof TwoColorPolygonBatch) {
         this.draw((TwoColorPolygonBatch)batch, skeleton);
      } else if (batch instanceof PolygonSpriteBatch) {
         this.draw((PolygonSpriteBatch)batch, skeleton);
      } else if (batch == null) {
         throw new IllegalArgumentException("batch cannot be null.");
      } else if (skeleton == null) {
         throw new IllegalArgumentException("skeleton cannot be null.");
      } else {
         BlendMode blendMode = null;
         float[] vertices = this.vertices.items;
         Color skeletonColor = skeleton.color;
         float r = skeleton.color.r;
         float g = skeletonColor.g;
         float b = skeletonColor.b;
         float a = skeletonColor.a;
         Object[] drawOrder = skeleton.drawOrder.items;
         int i = 0;

         for (int n = skeleton.drawOrder.size; i < n; i++) {
            Slot slot;
            if ((slot = (Slot)drawOrder[i]).bone.active) {
               Attachment attachment = slot.attachment;
               if (!(slot.attachment instanceof RegionAttachment)) {
                  if (attachment instanceof ClippingAttachment) {
                     throw new RuntimeException(
                        batch.getClass().getSimpleName() + " cannot perform clipping, PolygonSpriteBatch or TwoColorPolygonBatch is required."
                     );
                  }

                  if (attachment instanceof MeshAttachment) {
                     throw new RuntimeException(
                        batch.getClass().getSimpleName() + " cannot render meshes, PolygonSpriteBatch or TwoColorPolygonBatch is required."
                     );
                  }

                  if (attachment instanceof SkeletonAttachment) {
                     throw null;
                  }
               } else {
                  RegionAttachment region;
                  RegionAttachment regionAttachment = region = (RegionAttachment)attachment;
                  regionAttachment.computeWorldVertices(slot, vertices, 0, 5);
                  Color color = region.getColor();
                  Color slotColor = slot.color;
                  float alpha = a * slotColor.a * color.a * 255.0F;
                  SlotData slotData = slot.data;
                  BlendMode slotBlendMode;
                  if ((slotBlendMode = slot.data.blendMode) != blendMode) {
                     blendMode = slotBlendMode;
                     slotBlendMode.apply(batch, false);
                  }

                  int alphaInt = (int)alpha;
                  float c = NumberUtils.intToFloatColor(
                     alphaInt << 24
                        | (int)(b * slotColor.b * color.b * 255.0F) << 16
                        | (int)(g * slotColor.g * color.g * 255.0F) << 8
                        | (int)(r * slotColor.r * color.r * 255.0F)
                  );
                  float[] uvs = region.getUVs();
                  int u = 0;

                  for (int v = 2; u < 8; v += 5) {
                     vertices[v] = c;
                     vertices[v + 1] = uvs[u];
                     int vIndex = v + 2;
                     vertices[vIndex] = uvs[u + 1];
                     u += 2;
                  }

                  Texture texture = region.getRegion().getTexture();
                  batch.draw(texture, vertices, 0, 20);
               }
            }
         }
      }
   }

   public void draw(PolygonSpriteBatch batch, Skeleton skeleton) {
      if (batch == null) {
         throw new IllegalArgumentException("batch cannot be null.");
      } else if (skeleton == null) {
         throw new IllegalArgumentException("skeleton cannot be null.");
      } else {
         BlendMode blendMode = null;
         int verticesLength = 0;
         float[] vertices = null;
         float[] uvs = null;
         short[] triangles = null;
         Color color = null;
         Color skeletonColor = skeleton.color;
         float r = skeleton.color.r;
         float g = skeletonColor.g;
         float b = skeletonColor.b;
         float a = skeletonColor.a;
         Object[] drawOrder = skeleton.drawOrder.items;
         int i = 0;

         for (int n = skeleton.drawOrder.size; i < n; i++) {
            Slot slot;
            if ((slot = (Slot)drawOrder[i]).bone.active) {
               Texture texture = null;
               int tmpInt;
               if (this.clipper.isClipping()) {
                  tmpInt = 2;
               } else {
                  tmpInt = 5;
               }

               int vertexSize = tmpInt;
               Attachment attachment = slot.attachment;
               if (slot.attachment instanceof RegionAttachment) {
                  RegionAttachment region = (RegionAttachment)attachment;
                  verticesLength = vertexSize << 2;
                  vertices = this.vertices.items;
                  region.computeWorldVertices(slot, vertices, 0, vertexSize);
                  triangles = QUAD_TRIANGLES;
                  texture = region.getRegion().getTexture();
                  uvs = region.getUVs();
                  color = region.getColor();
               } else if (attachment instanceof MeshAttachment) {
                  int count;
                  MeshAttachment mesh;
                  verticesLength = ((count = (mesh = (MeshAttachment)attachment).getWorldVerticesLength()) >> 1) * vertexSize;
                  vertices = this.vertices.setSize(verticesLength);
                  mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
                  triangles = mesh.getTriangles();
                  texture = mesh.getRegion().getTexture();
                  uvs = mesh.getUVs();
                  color = mesh.getColor();
               } else {
                  if (attachment instanceof ClippingAttachment) {
                     ClippingAttachment clip = (ClippingAttachment)attachment;
                     this.clipper.clipStart(slot, clip);
                     continue;
                  }

                  if (attachment instanceof SkeletonAttachment) {
                     throw null;
                  }
               }

               if (texture != null) {
                  Color slotColor = slot.color;
                  float alpha = a * slotColor.a * color.a * 255.0F;
                  SlotData slotData = slot.data;
                  BlendMode slotBlendMode = slot.data.blendMode;
                  if (slot.data.blendMode != blendMode) {
                     blendMode = slotBlendMode;
                     slotBlendMode.apply(batch, false);
                  }

                  tmpInt = (int)alpha << 24 | (int)(b * slotColor.b * color.b * 255.0F) << 16;
                  int gInt = (int)(g * slotColor.g * color.g * 255.0F);
                  float c = NumberUtils.intToFloatColor(
                     tmpInt | gInt << 8 | (int)(r * slotColor.r * color.r * 255.0F)
                  );
                  if (this.clipper.isClipping()) {
                     this.clipper.clipTriangles(vertices, triangles, triangles.length, uvs, c, 0.0F, false);
                     FloatArray clippedVertices = this.clipper.getClippedVertices();
                     ShortArray clippedTriangles = this.clipper.getClippedTriangles();
                     batch.draw(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0, clippedTriangles.size);
                  } else {
                     int v = 2;

                     for (int u = 0; v < verticesLength; u += 2) {
                        vertices[v] = c;
                        vertices[v + 1] = uvs[u];
                        vertices[v + 2] = uvs[u + 1];
                        v += 5;
                     }

                     batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
                  }
               }
            }

            this.clipper.clipEndWithSlot(slot);
         }

         this.clipper.clipEnd();
      }
   }

   public void draw(TwoColorPolygonBatch batch, Skeleton skeleton) {
      if (batch == null) {
         throw new IllegalArgumentException("batch cannot be null.");
      } else if (skeleton == null) {
         throw new IllegalArgumentException("skeleton cannot be null.");
      } else {
         batch.setPremultipliedAlpha(false);
         BlendMode blendMode = null;
         int verticesLength = 0;
         float[] vertices = null;
         float[] uvs = null;
         short[] triangles = null;
         Color color = null;
         Color skeletonColor = skeleton.color;
         float r = skeleton.color.r;
         float g = skeletonColor.g;
         float b = skeletonColor.b;
         float a = skeletonColor.a;
         Object[] drawOrder = skeleton.drawOrder.items;
         int i = 0;

         for (int n = skeleton.drawOrder.size; i < n; i++) {
            Slot slot;
            if ((slot = (Slot)drawOrder[i]).bone.active) {
               Texture texture = null;
               int tmpInt;
               if (this.clipper.isClipping()) {
                  tmpInt = 2;
               } else {
                  tmpInt = 6;
               }

               int vertexSize = tmpInt;
               Attachment attachment = slot.attachment;
               if (slot.attachment instanceof RegionAttachment) {
                  RegionAttachment region = (RegionAttachment)attachment;
                  verticesLength = vertexSize << 2;
                  vertices = this.vertices.items;
                  region.computeWorldVertices(slot, vertices, 0, vertexSize);
                  triangles = QUAD_TRIANGLES;
                  texture = region.getRegion().getTexture();
                  uvs = region.getUVs();
                  color = region.getColor();
               } else if (attachment instanceof MeshAttachment) {
                  int count;
                  MeshAttachment mesh;
                  verticesLength = ((count = (mesh = (MeshAttachment)attachment).getWorldVerticesLength()) >> 1) * vertexSize;
                  vertices = this.vertices.setSize(verticesLength);
                  mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
                  triangles = mesh.getTriangles();
                  texture = mesh.getRegion().getTexture();
                  uvs = mesh.getUVs();
                  color = mesh.getColor();
               } else {
                  if (attachment instanceof ClippingAttachment) {
                     ClippingAttachment clip = (ClippingAttachment)attachment;
                     this.clipper.clipStart(slot, clip);
                     continue;
                  }

                  if (attachment instanceof SkeletonAttachment) {
                     throw null;
                  }
               }

               if (texture != null) {
                  Color slotColor = slot.color;
                  float alpha = a * slotColor.a * color.a * 255.0F;
                  SlotData slotData = slot.data;
                  BlendMode slotBlendMode = slot.data.blendMode;
                  if (slot.data.blendMode != blendMode) {
                     blendMode = slotBlendMode;
                     slotBlendMode.apply(batch, false);
                  }

                  float red = r * color.r * 255.0F;
                  float green = g * color.g * 255.0F;
                  float blue = b * color.b * 255.0F;
                  tmpInt = (int)alpha;
                  tmpInt <<= 24;
                  int tmpInt2 = (int)(blue * slotColor.b);
                  float light = NumberUtils.intToFloatColor(
                     tmpInt
                        | tmpInt2 << 16
                        | (int)(green * slotColor.g) << 8
                        | (int)(red * slotColor.r)
                  );
                  Color darkColor = slot.darkColor;
                  float darkValue;
                  if (slot.darkColor == null) {
                     darkValue = 0.0F;
                  } else {
                     tmpInt = (int)(blue * darkColor.b);
                     tmpInt <<= 16;
                     tmpInt2 = (int)(green * darkColor.g);
                     darkValue = NumberUtils.intToFloatColor(tmpInt | tmpInt2 << 8 | (int)(red * darkColor.r));
                  }

                  float dark = darkValue;
                  if (this.clipper.isClipping()) {
                     this.clipper.clipTriangles(vertices, triangles, triangles.length, uvs, light, dark, true);
                     FloatArray clippedVertices = this.clipper.getClippedVertices();
                     ShortArray clippedTriangles = this.clipper.getClippedTriangles();
                     batch.drawTwoColor(
                        texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0, clippedTriangles.size
                     );
                  } else {
                     int v = 2;

                     for (int u = 0; v < verticesLength; u += 2) {
                        vertices[v] = light;
                        vertices[v + 1] = dark;
                        vertices[v + 2] = uvs[u];
                        tmpInt2 = v + 3;
                        vertices[tmpInt2] = uvs[u + 1];
                        v += 6;
                     }

                     batch.drawTwoColor(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
                  }
               }
            }

            this.clipper.clipEndWithSlot(slot);
         }

         this.clipper.clipEnd();
      }
   }

   static {
      short[] quadTriangles = new short[6];
      quadTriangles[0] = (short)0;
      quadTriangles[1] = (short)1;
      quadTriangles[2] = (short)2;
      quadTriangles[3] = (short)2;
      quadTriangles[4] = (short)3;
      quadTriangles[5] = (short)0;
      QUAD_TRIANGLES = quadTriangles;
   }

}
