package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.BoneData;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.Updatable;
import com.tlnt.thirdparty.spine.utils.SpineUtils;

public final class Bone implements Updatable {
   public final BoneData data;
   public final Skeleton skeleton;
   @Null
   public final Bone parent;
   public final Array<Bone> children = new Array<>();
   public float x;
   public float y;
   public float rotation;
   public float scaleX;
   public float scaleY;
   public float shearX;
   public float shearY;
   public float ax;
   public float ay;
   public float arotation;
   public float ascaleX;
   public float ascaleY;
   public float ashearX;
   public float ashearY;
   public float a;
   public float b;
   public float worldX;
   public float c;
   public float d;
   public float worldY;
   public int inherit;
   public boolean sorted;
   public boolean active;

   public Bone(BoneData data, Skeleton skeleton, @Null Bone parent) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
         this.skeleton = skeleton;
         this.parent = parent;
         BoneData setupData = this.data;
         this.x = setupData.x;
         this.y = setupData.y;
         this.rotation = setupData.rotation;
         this.scaleX = setupData.scaleX;
         this.scaleY = setupData.scaleY;
         this.shearX = setupData.shearX;
         this.shearY = setupData.shearY;
         this.inherit = setupData.inherit;
      }
   }

   @Override
   public final void update(int physics) {
      this.updateWorldTransform(this.ax, this.ay, this.arotation, this.ascaleX, this.ascaleY, this.ashearX, this.ashearY);
   }

   // [recovered via CFR from reference bytecode; Vineflower failed on this method]
   public final void updateWorldTransform(float x, float y, float rotation, float scaleX, float scaleY, float shearX, float shearY) {
       this.ax = x;
       this.ay = y;
       this.arotation = rotation;
       this.ascaleX = scaleX;
       this.ascaleY = scaleY;
       this.ashearX = shearX;
       this.ashearY = shearY;
       Bone parent = this.parent;
       if (parent == null) {
           Skeleton skeleton = this.skeleton;
           float sx = skeleton.scaleX;
           float sy = skeleton.scaleY;
           float rx = (rotation + shearX) * ((float)Math.PI / 180);
           float ry = (rotation + 90.0f + shearY) * ((float)Math.PI / 180);
           float angle = rx;
           this.a = (float)Math.cos(angle) * scaleX * sx;
           angle = ry;
           this.b = (float)Math.cos(angle) * scaleY * sx;
           angle = rx;
           this.c = (float)Math.sin(angle) * scaleX * sy;
           angle = ry;
           this.d = (float)Math.sin(angle) * scaleY * sy;
           this.worldX = x * sx + skeleton.x;
           this.worldY = y * sy + skeleton.y;
           return;
       }
       float pa = parent.a;
       float pb = parent.b;
       float pc = parent.c;
       float pd = parent.d;
       this.worldX = pa * x + pb * y + parent.worldX;
       this.worldY = pc * x + pd * y + parent.worldY;
       switch (this.inherit) {
           case BoneData.Inherit.NORMAL: {
               float rx = (rotation + shearX) * ((float)Math.PI / 180);
               x = (rotation + 90.0f + shearY) * ((float)Math.PI / 180);
               float angle = rx;
               y = (float)Math.cos(angle) * scaleX;
               angle = x;
               float lb = (float)Math.cos(angle) * scaleY;
               angle = rx;
               rx = (float)Math.sin(angle) * scaleX;
               angle = x;
               x = (float)Math.sin(angle) * scaleY;
               this.a = pa * y + pb * rx;
               this.b = pa * lb + pb * x;
               this.c = pc * y + pd * rx;
               this.d = pc * lb + pd * x;
               return;
           }
           case BoneData.Inherit.ONLY_TRANSLATION: {
               float rx = (rotation + shearX) * ((float)Math.PI / 180);
               x = (rotation + 90.0f + shearY) * ((float)Math.PI / 180);
               float angle = rx;
               this.a = (float)Math.cos(angle) * scaleX;
               angle = x;
               this.b = (float)Math.cos(angle) * scaleY;
               angle = rx;
               this.c = (float)Math.sin(angle) * scaleX;
               angle = x;
               this.d = (float)Math.sin(angle) * scaleY;
               break;
           }
           case BoneData.Inherit.NO_ROTATION_OR_REFLECTION: {
               float prx;
               float unused;
               float sx = 1.0f / this.skeleton.scaleX;
               x = 1.0f / this.skeleton.scaleY;
               float paCopy = pa *= sx;
               float pcCopy = pc *= x;
               y = paCopy * paCopy + pcCopy * pcCopy;
               if (y > 1.0E-4f) {
                   y = Math.abs(pa * pd * x - pb * sx * pc) / y;
                   pb = pc * y;
                   pd = pa * y;
                   prx = SpineUtils.atan2Deg(pc, pa);
               } else {
                   pa = 0.0f;
                   pc = 0.0f;
                   prx = 90.0f - SpineUtils.atan2Deg(pd, pb);
               }
               sx = (rotation + shearX - prx) * ((float)Math.PI / 180);
               x = (rotation + shearY - prx + 90.0f) * ((float)Math.PI / 180);
               float angle = sx;
               rotation = (float)Math.cos(angle) * scaleX;
               angle = x;
               float lb = (float)Math.cos(angle) * scaleY;
               angle = sx;
               sx = (float)Math.sin(angle) * scaleX;
               angle = x;
               scaleX = (float)Math.sin(angle) * scaleY;
               this.a = pa * rotation - pb * sx;
               this.b = pa * lb - pb * scaleX;
               this.c = pc * rotation + pd * sx;
               this.d = pc * lb + pd * scaleX;
               break;
           }
           case BoneData.Inherit.NO_SCALE: 
           case BoneData.Inherit.NO_SCALE_OR_REFLECTION: {
               float unused;
               float angle = rotation *= (float)Math.PI / 180;
               float cos = (float)Math.cos(rotation);
               angle = rotation;
               x = (float)Math.sin(angle);
               y = (pa * cos + pb * x) / this.skeleton.scaleX;
               float zc = (pc * cos + pd * x) / this.skeleton.scaleY;
               float zaCopy = y;
               float zcCopy = zc;
               cos = (float)Math.sqrt(zaCopy * zaCopy + zcCopy * zcCopy);
               if (cos > 1.0E-5f) {
                   cos = 1.0f / cos;
               }
               float zaNorm = y *= cos;
               float zcNorm = zc *= cos;
               cos = (float)Math.sqrt(zaNorm * zaNorm + zcNorm * zcNorm);
               if (this.inherit == BoneData.Inherit.NO_SCALE) {
                   int skeletonReflect;
                   int syNeg;
                   int sxNeg;
                   int parentReflect;
                   if (pa * pd - pb * pc < 0.0f) {
                       parentReflect = 1;
                   } else {
                       parentReflect = 0;
                   }
                   if (this.skeleton.scaleX < 0.0f) {
                       sxNeg = 1;
                   } else {
                       sxNeg = 0;
                   }
                   if (this.skeleton.scaleY < 0.0f) {
                       syNeg = 1;
                   } else {
                       syNeg = 0;
                   }
                   if (sxNeg != syNeg) {
                       skeletonReflect = 1;
                   } else {
                       skeletonReflect = 0;
                   }
                   if (parentReflect != skeletonReflect) {
                       cos = -cos;
                   }
               }
               angle = rotation = 1.5707964f + SpineUtils.atan2(zc, y);
               x = (float)Math.cos(rotation) * cos;
               angle = rotation;
               rotation = (float)Math.sin(angle) * cos;
               shearY = (shearY + 90.0f) * ((float)Math.PI / 180);
               angle = shearX *= (float)Math.PI / 180;
               float la = (float)Math.cos(angle) * scaleX;
               angle = shearY;
               cos = (float)Math.cos(angle) * scaleY;
               angle = shearX;
               scaleX = (float)Math.sin(angle) * scaleX;
               angle = shearY;
               scaleY = (float)Math.sin(angle) * scaleY;
               this.a = y * la + x * scaleX;
               this.b = y * cos + x * scaleY;
               this.c = zc * la + rotation * scaleX;
               this.d = zc * cos + rotation * scaleY;
           }
       }
       this.a *= this.skeleton.scaleX;
       this.b *= this.skeleton.scaleX;
       this.c *= this.skeleton.scaleY;
       this.d *= this.skeleton.scaleY;
   }

   public final void setRotation(float rotation) {
      this.rotation = rotation;
   }

   // [recovered via CFR from reference bytecode; Vineflower failed on this method]
   public final void updateAppliedTransform() {
       Bone parent = this.parent;
       if (parent == null) {
           this.ax = this.worldX - this.skeleton.x;
           this.ay = this.worldY - this.skeleton.y;
           float a = this.a;
           float b = this.b;
           float c = this.c;
           float d = this.d;
           this.arotation = SpineUtils.atan2Deg(c, a);
           float aCopy = a;
           float cCopy = c;
           this.ascaleX = (float)Math.sqrt(aCopy * aCopy + cCopy * cCopy);
           float bCopy = b;
           float dCopy = d;
           this.ascaleY = (float)Math.sqrt(bCopy * bCopy + dCopy * dCopy);
           this.ashearX = 0.0f;
           this.ashearY = SpineUtils.atan2Deg(a * b + c * d, a * d - b * c);
           return;
       }
       float pa = parent.a;
       float pb = parent.b;
       float pc = parent.c;
       float pd = parent.d;
       float pid = 1.0f / (pa * pd - pb * pc);
       float ia = pd * pid;
       float ib = pb * pid;
       float ic = pc * pid;
       float id = pa * pid;
       float dx = this.worldX - parent.worldX;
       float dy = this.worldY - parent.worldY;
       this.ax = dx * ia - dy * ib;
       this.ay = dy * id - dx * ic;
       if (this.inherit == BoneData.Inherit.ONLY_TRANSLATION) {
           dy = this.a;
           pa = this.b;
           pb = this.c;
           pc = this.d;
       } else {
           switch (this.inherit) {
               case BoneData.Inherit.NO_ROTATION_OR_REFLECTION: {
                   float paCopy = pa;
                   float pcCopy = pc;
                   ia = Math.abs(pa * pd - pb * pc) / (paCopy * paCopy + pcCopy * pcCopy);
                   pb = -pc * this.skeleton.scaleX * ia / this.skeleton.scaleY;
                   pd = pa * this.skeleton.scaleY * ia / this.skeleton.scaleX;
                   pid = 1.0f / (pa * pd - pb * pc);
                   ia = pd * pid;
                   ib = pb * pid;
                   break;
               }
               case BoneData.Inherit.NO_SCALE: 
               case BoneData.Inherit.NO_SCALE_OR_REFLECTION: {
                   float unused;
                   ib = ia = this.rotation * ((float)Math.PI / 180);
                   dy = (float)Math.cos(ia);
                   ib = ia;
                   ia = (float)Math.sin(ib);
                   pa = (pa * dy + pb * ia) / this.skeleton.scaleX;
                   pc = (pc * dy + pd * ia) / this.skeleton.scaleY;
                   float paRot = pa;
                   float pcRot = pc;
                   dy = (float)Math.sqrt(paRot * paRot + pcRot * pcRot);
                   if (dy > 1.0E-5f) {
                       dy = 1.0f / dy;
                   }
                   float paNorm = pa *= dy;
                   float pcNorm = pc *= dy;
                   dy = (float)Math.sqrt(paNorm * paNorm + pcNorm * pcNorm);
                   if (this.inherit == BoneData.Inherit.NO_SCALE) {
                       int skeletonReflect;
                       int syNeg;
                       int sxNeg;
                       int parentReflect;
                       if (pid < 0.0f) {
                           parentReflect = 1;
                       } else {
                           parentReflect = 0;
                       }
                       if (this.skeleton.scaleX < 0.0f) {
                           sxNeg = 1;
                       } else {
                           sxNeg = 0;
                       }
                       if (this.skeleton.scaleY < 0.0f) {
                           syNeg = 1;
                       } else {
                           syNeg = 0;
                       }
                       if (sxNeg != syNeg) {
                           skeletonReflect = 1;
                       } else {
                           skeletonReflect = 0;
                       }
                       if (parentReflect != skeletonReflect) {
                           dy = -dy;
                       }
                   }
                   ib = ia = 1.5707964f + SpineUtils.atan2(pc, pa);
                   pb = (float)Math.cos(ia) * dy;
                   ib = ia;
                   pd = (float)Math.sin(ib) * dy;
                   pid = 1.0f / (pa * pd - pb * pc);
                   ia = pd * pid;
                   ib = pb * pid;
                   ic = pc * pid;
                   id = pa * pid;
               }
           }
           dy = ia * this.a - ib * this.c;
           pa = ia * this.b - ib * this.d;
           pb = id * this.c - ic * this.a;
           pc = id * this.d - ic * this.b;
       }
       this.ashearX = 0.0f;
       float raCopy = dy;
       float rcCopy = pb;
       this.ascaleX = (float)Math.sqrt(raCopy * raCopy + rcCopy * rcCopy);
       if (this.ascaleX > 1.0E-4f) {
           ia = dy * pc - pa * pb;
           this.ascaleY = ia / this.ascaleX;
           this.ashearY = -SpineUtils.atan2Deg(dy * pa + pb * pc, ia);
           this.arotation = SpineUtils.atan2Deg(pb, dy);
           return;
       }
       this.ascaleX = 0.0f;
       float rbCopy = pa;
       float rdCopy = pc;
       this.ascaleY = (float)Math.sqrt(rbCopy * rbCopy + rdCopy * rdCopy);
       this.ashearY = 0.0f;
       this.arotation = 90.0f - SpineUtils.atan2Deg(pc, pa);
   }

   public final float getA() {
      return this.a;
   }

   public final float getB() {
      return this.b;
   }

   public final float getC() {
      return this.c;
   }

   public final float getD() {
      return this.d;
   }

   public final float getWorldX() {
      return this.worldX;
   }

   public final float getWorldY() {
      return this.worldY;
   }

   public final float getWorldScaleX() {
      return (float)Math.sqrt((double)(this.a * this.a + this.c * this.c));
   }

   public final Vector2 localToWorld(Vector2 local) {
      if (local == null) {
         throw new IllegalArgumentException("local cannot be null.");
      } else {
         float x = local.x;
         float y = local.y;
         local.x = x * this.a + y * this.b + this.worldX;
         local.y = x * this.c + y * this.d + this.worldY;
         return local;
      }
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

}
