package com.tlnt.thirdparty.spine;

import com.tlnt.thirdparty.spine.Bone;
import com.tlnt.thirdparty.spine.PhysicsConstraintData;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.Updatable;
import com.tlnt.thirdparty.spine.utils.SpineUtils;

public final class PhysicsConstraint implements Updatable {
   public final PhysicsConstraintData data;
   public Bone bone;
   public float inertia;
   public float strength;
   public float damping;
   public float massInverse;
   public float wind;
   public float gravity;
   public float mix;
   public boolean reset;
   public float ux;
   public float uy;
   public float cx;
   public float cy;
   public float tx;
   public float ty;
   public float xOffset;
   public float xVelocity;
   public float yOffset;
   public float yVelocity;
   public float rotateOffset;
   public float rotateVelocity;
   public float scaleOffset;
   public float scaleVelocity;
   public boolean active;
   public Skeleton skeleton;
   public float remaining;
   public float lastTime;

   public PhysicsConstraint(PhysicsConstraintData data, Skeleton skeleton) {
      this.reset = true;
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
         this.skeleton = skeleton;
         this.bone = skeleton.bones.get(data.bone.index);
         this.inertia = data.inertia;
         this.strength = data.strength;
         this.damping = data.damping;
         this.massInverse = data.massInverse;
         this.wind = data.wind;
         this.gravity = data.gravity;
         this.mix = data.mix;
      }
   }

   public final void reset() {
      this.remaining = 0.0F;
      this.lastTime = this.skeleton.time;
      this.reset = true;
      this.xOffset = 0.0F;
      this.xVelocity = 0.0F;
      this.yOffset = 0.0F;
      this.yVelocity = 0.0F;
      this.rotateOffset = 0.0F;
      this.rotateVelocity = 0.0F;
      this.scaleOffset = 0.0F;
      this.scaleVelocity = 0.0F;
   }

   @Override
   // [recovered via CFR from reference bytecode; Vineflower failed on this method]
   public final void update(int physics) {
       float qy;
       float a;
       float by;
       float bx;
       float delta;
       int rotateFlag;
       int y;
       int x;
       float mix = this.mix;
       if (mix == 0.0f) {
           return;
       }
       if (this.data.x > 0.0f) {
           x = 1;
       } else {
           x = 0;
       }
       if (this.data.y > 0.0f) {
           y = 1;
       } else {
           y = 0;
       }
       if (this.data.rotate > 0.0f || this.data.shearX > 0.0f) {
           rotateFlag = 1;
       } else {
           rotateFlag = 0;
       }
       int rotateOrShearX = rotateFlag;
       int scaleX = this.data.scaleX > 0.0f ? 1 : 0;
       Bone bone = this.bone;
       float l = bone.data.length;
       switch (physics) {
           case Skeleton.Physics.NONE: {
               return;
           }
           case Skeleton.Physics.RESET: {
               this.reset();
           }
           case Skeleton.Physics.UPDATE: {
               Skeleton skeleton = this.skeleton;
               delta = Math.max(skeleton.time - this.lastTime, 0.0f);
               this.remaining += delta;
               this.lastTime = skeleton.time;
               bx = bone.worldX;
               by = bone.worldY;
               if (this.reset) {
                   this.reset = false;
                   this.ux = bx;
                   this.uy = by;
               } else {
                   float u;
                   a = this.remaining;
                   float i = this.inertia;
                   float t = this.data.step;
                   float f = skeleton.data.referenceScale;
                   float d = -1.0f;
                   delta = this.data.limit * delta;
                   qy = delta * Math.abs(skeleton.scaleY);
                   delta *= Math.abs(skeleton.scaleX);
                   if (x != 0 || y != 0) {
                       if (x != 0) {
                           u = (this.ux - bx) * i;
                           this.xOffset += u > delta ? delta : (u < -delta ? -delta : u);
                           this.ux = bx;
                       }
                       if (y != 0) {
                           u = (this.uy - by) * i;
                           this.yOffset += u > qy ? qy : (u < -qy ? -qy : u);
                           this.uy = by;
                       }
                       if (a >= t) {
                           d = (float)Math.pow(this.damping, 60.0f * t);
                           u = this.massInverse * t;
                           bx = this.strength;
                           by = this.wind * f * skeleton.scaleX;
                           float g = this.gravity * f * skeleton.scaleY;
                           do {
                               if (x != 0) {
                                   this.xVelocity += (by - this.xOffset * bx) * u;
                                   this.xOffset += this.xVelocity * t;
                                   this.xVelocity *= d;
                               }
                               if (y == 0) continue;
                               this.yVelocity -= (g + this.yOffset * bx) * u;
                               this.yOffset += this.yVelocity * t;
                               this.yVelocity *= d;
                           } while ((a -= t) >= t);
                       }
                       if (x != 0) {
                           bone.worldX += this.xOffset * mix * this.data.x;
                       }
                       if (y != 0) {
                           bone.worldY += this.yOffset * mix * this.data.y;
                       }
                   }
                   if (rotateOrShearX != 0 || scaleX != 0) {
                       float unused;
                       u = SpineUtils.atan2(bone.c, bone.a);
                       float mr = 0.0f;
                       float dx = this.cx - bone.worldX;
                       float dy = this.cy - bone.worldY;
                       if (dx > delta) {
                           dx = delta;
                       } else if (dx < -delta) {
                           dx = -delta;
                       }
                       if (dy > qy) {
                           dy = qy;
                       } else if (dy < -qy) {
                           dy = -qy;
                       }
                       if (rotateOrShearX != 0) {
                           mr = (this.data.rotate + this.data.shearX) * mix;
                           float r = delta = SpineUtils.atan2(dy + this.ty, dx + this.tx) - u - this.rotateOffset * mr;
                           this.rotateOffset += (r - (float)Math.ceil(r * 0.15915494f - 0.5f) * ((float)Math.PI * 2)) * i;
                           qy = delta = this.rotateOffset * mr + u;
                           bx = (float)Math.cos(delta);
                           qy = delta;
                           by = (float)Math.sin(qy);
                           if (scaleX != 0) {
                               float unusedR;
                               delta = l * bone.getWorldScaleX();
                               if (delta > 0.0f) {
                                   this.scaleOffset += (dx * bx + dy * by) * i / delta;
                               }
                           }
                       } else {
                           float unusedR2;
                           qy = u;
                           bx = (float)Math.cos(qy);
                           qy = u;
                           by = (float)Math.sin(qy);
                           delta = l * bone.getWorldScaleX();
                           if (delta > 0.0f) {
                               this.scaleOffset += (dx * bx + dy * by) * i / delta;
                           }
                       }
                       a = this.remaining;
                       if (a >= t) {
                           if (d == -1.0f) {
                               d = (float)Math.pow(this.damping, 60.0f * t);
                           }
                           delta = this.massInverse * t;
                           dx = this.strength;
                           dy = this.wind;
                           i = this.gravity;
                           f = l / f;
                           while (true) {
                               a -= t;
                               if (scaleX != 0) {
                                   this.scaleVelocity += (dy * bx - i * by - this.scaleOffset * dx) * delta;
                                   this.scaleOffset += this.scaleVelocity * t;
                                   this.scaleVelocity *= d;
                               }
                               if (rotateOrShearX != 0) {
                                   this.rotateVelocity -= ((dy * by + i * bx) * f + this.rotateOffset * dx) * delta;
                                   this.rotateOffset += this.rotateVelocity * t;
                                   this.rotateVelocity *= d;
                                   if (a < t) break;
                                   qy = by = this.rotateOffset * mr + u;
                                   bx = (float)Math.cos(by);
                                   qy = by;
                                   by = (float)Math.sin(qy);
                                   continue;
                               }
                               if (a < t) break;
                           }
                       }
                   }
                   this.remaining = a;
               }
               this.cx = bone.worldX;
               this.cy = bone.worldY;
               break;
           }
           case Skeleton.Physics.POSE: {
               if (x != 0) {
                   bone.worldX += this.xOffset * mix * this.data.x;
               }
               if (y == 0) break;
               bone.worldY += this.yOffset * mix * this.data.y;
           }
       }
       if (rotateOrShearX != 0) {
           float o = this.rotateOffset * mix;
           if (this.data.shearX > 0.0f) {
               a = 0.0f;
               if (this.data.rotate > 0.0f) {
                   qy = a = o * this.data.rotate;
                   delta = (float)Math.sin(a);
                   qy = a;
                   bx = (float)Math.cos(qy);
                   by = bone.b;
                   bone.b = bx * by - delta * bone.d;
                   bone.d = delta * by + bx * bone.d;
               }
               qy = a += o * this.data.shearX;
               delta = (float)Math.sin(a);
               qy = a;
               bx = (float)Math.cos(qy);
               by = bone.a;
               bone.a = bx * by - delta * bone.c;
               bone.c = delta * by + bx * bone.c;
           } else {
               qy = o *= this.data.rotate;
               delta = (float)Math.sin(o);
               qy = o;
               bx = (float)Math.cos(qy);
               by = bone.a;
               bone.a = bx * by - delta * bone.c;
               bone.c = delta * by + bx * bone.c;
               by = bone.b;
               bone.b = bx * by - delta * bone.d;
               bone.d = delta * by + bx * bone.d;
           }
       }
       if (scaleX != 0) {
           float s = 1.0f + this.scaleOffset * mix * this.data.scaleX;
           bone.a *= s;
           bone.c *= s;
       }
       if (physics != Skeleton.Physics.POSE) {
           this.tx = l * bone.a;
           this.ty = l * bone.c;
       }
       bone.updateAppliedTransform();
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

}
