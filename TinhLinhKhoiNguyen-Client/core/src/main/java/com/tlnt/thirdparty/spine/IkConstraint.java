package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;
import com.tlnt.thirdparty.spine.utils.SpineUtils;

public final class IkConstraint implements Updatable {
   public final IkConstraintData data;
   public final Array<Bone> bones;
   public Bone target;
   public int bendDirection;
   public boolean compress;
   public boolean stretch;
   public float mix = 1.0F;
   public float softness;
   public boolean active;

   public IkConstraint(IkConstraintData data, Skeleton skeleton) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
         this.bones = new Array<>(data.bones.size);

         for (BoneData boneData : data.bones) {
            this.bones.add(skeleton.bones.get(boneData.index));
         }

         this.target = skeleton.bones.get(data.target.index);
         this.mix = data.mix;
         this.softness = data.softness;
         this.bendDirection = data.bendDirection;
         this.compress = data.compress;
         this.stretch = data.stretch;
      }
   }

   @Override
   public final void update(int physics) {
      if (this.mix != 0.0F) {
         Bone target = this.target;
         Object[] bones = this.bones.items;
         switch (this.bones.size) {
            case 1:
               apply((Bone)bones[0], target.worldX, target.worldY, this.compress, this.stretch, this.data.uniform, this.mix);
               return;
            case 2:
               Bone bone0 = (Bone)bones[0];
               Bone bone1 = (Bone)bones[1];
               float alpha = this.mix;
               float softness = this.softness;
               boolean uniform = this.data.uniform;
               boolean stretch = this.stretch;
               int bendDir = this.bendDirection;
               float targetY = target.worldY;
               float targetX = target.worldX;
               Bone child = bone1;
               Bone parent = bone0;
               if (bone0 == null) {
                  throw new IllegalArgumentException("parent cannot be null.");
               } else if (child == null) {
                  throw new IllegalArgumentException("child cannot be null.");
               } else if (parent.inherit != BoneData.Inherit.NORMAL || child.inherit != BoneData.Inherit.NORMAL) {
                  return;
               } else {
                  float px = parent.ax;
                  float py = parent.ay;
                  float psx = parent.ascaleX;
                  float psy = parent.ascaleY;
                  float sx = psx;
                  float sy = psy;
                  float csx = child.ascaleX;
                  int os1;
                  int s2;
                  if (psx < 0.0F) {
                     psx = -psx;
                     os1 = 180;
                     s2 = -1;
                  } else {
                     os1 = 0;
                     s2 = 1;
                  }

                  if (psy < 0.0F) {
                     psy = -psy;
                     s2 = -s2;
                  }

                  int os2;
                  if (csx < 0.0F) {
                     csx = -csx;
                     os2 = 180;
                  } else {
                     os2 = 0;
                  }

                  float cx = child.ax;
                  float a = parent.a;
                  float b = parent.b;
                  float c = parent.c;
                  float d = parent.d;
                  int uFlag;
                  if (Math.abs(psx - psy) <= 1.0E-4F) {
                     uFlag = 1;
                  } else {
                     uFlag = 0;
                  }

                  int u = uFlag;
                  float cy;
                  float cwx;
                  float cwy;
                  if (uFlag != 0 && !stretch) {
                     cy = child.ay;
                     cwx = a * cx + b * cy + parent.worldX;
                     cwy = c * cx + d * cy + parent.worldY;
                  } else {
                     cy = 0.0F;
                     cwx = a * cx + parent.worldX;
                     cwy = c * cx + parent.worldY;
                  }

                  Bone pp = parent.parent;
                  a = parent.parent.a;
                  b = pp.b;
                  c = pp.c;
                  d = pp.d;
                  float id = a * d - b * c;
                  cwx -= pp.worldX;
                  cwy -= pp.worldY;
                  id = Math.abs(id) <= 1.0E-4F ? 0.0F : 1.0F / id;
                  float l1 = (cwx * d - cwy * b) * id - px;
                  cwx = (cwy * a - cwx * c) * id - py;
                  l1 = (float)Math.sqrt((double)(l1 * l1 + cwx * cwx));
                  float l2 = child.data.length * csx;
                  if (l1 < 1.0E-4F) {
                     apply(parent, targetX, targetY, false, stretch, false, alpha);
                     child.updateWorldTransform(cx, cy, 0.0F, child.ascaleX, child.ascaleY, child.ashearX, child.ashearY);
                     return;
                  } else {
                     cwx = targetX - pp.worldX;
                     cwy = targetY - pp.worldY;
                     targetX = (cwx * d - cwy * b) * id - px;
                     cwx = (cwy * a - cwx * c) * id - py;
                     float dd = targetX * targetX + cwx * cwx;
                     if (softness != 0.0F) {
                        softness *= psx * (csx + 1.0F) * 0.5F;
                        if ((cwy = (csx = (float)Math.sqrt((double)dd)) - l1 - l2 * psx + softness) > 0.0F) {
                           targetY = Math.min(1.0F, cwy / (softness * 2.0F)) - 1.0F;
                           targetY = (cwy - softness * (1.0F - targetY * targetY)) / csx;
                           targetX -= targetY * targetX;
                           cwx -= targetY * cwx;
                           dd = targetX * targetX + cwx * cwx;
                        }
                     }

                     if (u != 0) {
                        l2 *= psx;
                        if ((csx = (dd - l1 * l1 - l2 * l2) / (l1 * 2.0F * l2)) < -1.0F) {
                           csx = -1.0F;
                           targetY = (float) Math.PI * (float)bendDir;
                        } else if (csx > 1.0F) {
                           csx = 1.0F;
                           targetY = 0.0F;
                           if (stretch) {
                              a = ((float)Math.sqrt((double)dd) / (l1 + l2) - 1.0F) * alpha + 1.0F;
                              sx *= a;
                              if (uniform) {
                                 sy *= a;
                              }
                           }
                        } else {
                           targetY = (float)Math.acos((double)csx) * (float)bendDir;
                        }

                        a = l1 + l2 * csx;
                        b = l2 * (float)Math.sin((double)targetY);
                        targetX = SpineUtils.atan2(cwx * a - targetX * b, targetX * a + cwx * b);
                     } else {
                        label147: {
                           a = psx * l2;
                           b = psy * l2;
                           csx = a * a;
                           cwy = b * b;
                           targetY = SpineUtils.atan2(cwx, targetX);
                           c = cwy * l1 * l1 + csx * dd - csx * cwy;
                           targetX = -2.0F * cwy * l1;
                           float c2 = cwy - csx;
                           if ((d = targetX * targetX - 4.0F * c2 * c) >= 0.0F) {
                              float sqrtD = (float)Math.sqrt((double)d);
                              if (targetX < 0.0F) {
                                 sqrtD = -sqrtD;
                              }

                              float q;
                              targetX = (q = -(targetX + sqrtD) * 0.5F) / c2;
                              float r1 = c / q;
                              softness = Math.abs(targetX) < Math.abs(r1) ? targetX : r1;
                              if ((targetX = dd - softness * softness) >= 0.0F) {
                                 cwy = (float)Math.sqrt((double)targetX) * (float)bendDir;
                                 targetX = targetY - SpineUtils.atan2(cwy, softness);
                                 targetY = SpineUtils.atan2(cwy / psy, (softness - l1) / psx);
                                 break label147;
                              }
                           }

                           float minAngle = (float) Math.PI;
                           float minDist = (targetX = l1 - a) * targetX;
                           softness = 0.0F;
                           psx = 0.0F;
                           float maxDist = (psy = l1 + a) * psy;
                           id = 0.0F;
                           if ((c = -a * l1 / (csx - cwy)) >= -1.0F && c <= 1.0F) {
                              c = (float)Math.acos((double)c);
                              cwx = a * (float)Math.cos((double)c) + l1;
                              cwy = b * (float)Math.sin((double)c);
                              if ((d = cwx * cwx + cwy * cwy) < minDist) {
                                 minAngle = c;
                                 minDist = d;
                                 targetX = cwx;
                                 softness = cwy;
                              }

                              if (d > maxDist) {
                                 psx = c;
                                 maxDist = d;
                                 psy = cwx;
                                 id = cwy;
                              }
                           }

                           if (dd <= (minDist + maxDist) * 0.5F) {
                              targetX = targetY - SpineUtils.atan2(softness * (float)bendDir, targetX);
                              targetY = minAngle * (float)bendDir;
                           } else {
                              targetX = targetY - SpineUtils.atan2(id * (float)bendDir, psy);
                              targetY = psx * (float)bendDir;
                           }
                        }
                     }

                     csx = SpineUtils.atan2(cy, cx) * (float)s2;
                     cwy = parent.arotation;
                     if ((targetX = (targetX - csx) * (180.0F / (float)Math.PI) + (float)os1 - cwy) > 180.0F) {
                        targetX -= 360.0F;
                     } else if (targetX < -180.0F) {
                        targetX += 360.0F;
                     }

                     parent.updateWorldTransform(px, py, cwy + targetX * alpha, sx, sy, 0.0F, 0.0F);
                     cwy = child.arotation;
                     if ((targetY = ((targetY + csx) * (180.0F / (float)Math.PI) - child.ashearX) * (float)s2 + (float)os2 - cwy) > 180.0F) {
                        targetY -= 360.0F;
                     } else if (targetY < -180.0F) {
                        targetY += 360.0F;
                     }

                     child.updateWorldTransform(cx, cy, cwy + targetY * alpha, child.ascaleX, child.ascaleY, child.ashearX, child.ashearY);
                  }
               }
         }
      }
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

   // [recovered via CFR from reference bytecode; Vineflower failed on this method]
   public static void apply(Bone bone, float targetX, float targetY, boolean compress, boolean stretch, boolean uniform, float alpha) {
       float sa;
       float rotationIK;
       float pd;
       block17: {
           float x;
           float sc;
           block19: {
               block18: {
                   float unused;
                   float tx;
                   if (bone == null) {
                       throw new IllegalArgumentException("bone cannot be null.");
                   }
                   Bone p = bone.parent;
                   float pa = p.a;
                   float pb = p.b;
                   float pc = p.c;
                   pd = p.d;
                   rotationIK = -bone.ashearX - bone.arotation;
                   switch (bone.inherit) {
                       case BoneData.Inherit.ONLY_TRANSLATION: {
                           tx = (targetX - bone.worldX) * Math.signum(bone.skeleton.scaleX);
                           pa = (targetY - bone.worldY) * Math.signum(bone.skeleton.scaleY);
                           break;
                       }
                       case BoneData.Inherit.NO_ROTATION_OR_REFLECTION: {
                           float paTmp = pa;
                           float pcTmp = pc;
                           pd = Math.abs(pa * pd - pb * pc) / Math.max(1.0E-4f, paTmp * paTmp + pcTmp * pcTmp);
                           sa = pa / bone.skeleton.scaleX;
                           sc = pc / bone.skeleton.scaleY;
                           pb = -sc * pd * bone.skeleton.scaleX;
                           pd = sa * pd * bone.skeleton.scaleY;
                           rotationIK += SpineUtils.atan2Deg(sc, sa);
                       }
                       default: {
                           float unused2;
                           x = targetX - p.worldX;
                           sc = targetY - p.worldY;
                           sa = pa * pd - pb * pc;
                           if (Math.abs(sa) <= 1.0E-4f) {
                               tx = 0.0f;
                               pa = 0.0f;
                               break;
                           }
                           tx = (x * pd - sc * pb) / sa - bone.ax;
                           pa = (sc * pa - x * pc) / sa - bone.ay;
                       }
                   }
                   rotationIK += SpineUtils.atan2Deg(pa, tx);
                   if (bone.ascaleX < 0.0f) {
                       rotationIK += 180.0f;
                   }
                   if (rotationIK > 180.0f) {
                       rotationIK -= 360.0f;
                   } else if (rotationIK < -180.0f) {
                       rotationIK += 360.0f;
                   }
                   pd = bone.ascaleX;
                   sa = bone.ascaleY;
                   if (!compress && !stretch) break block17;
                   switch (bone.inherit) {
                       case BoneData.Inherit.NO_SCALE: 
                       case BoneData.Inherit.NO_SCALE_OR_REFLECTION: {
                           tx = targetX - bone.worldX;
                           pa = targetY - bone.worldY;
                       }
                   }
                   sc = bone.data.length * pd;
                   if (!(sc > 1.0E-4f)) break block17;
                   float txTmp = tx;
                   float tyTmp = pa;
                   x = txTmp * txTmp + tyTmp * tyTmp;
                   if (!compress) break block18;
                   float bTmp = sc;
                   if (x < bTmp * bTmp) break block19;
               }
               if (!stretch) break block17;
               float bTmp2 = sc;
               if (!(x > bTmp2 * bTmp2)) break block17;
           }
           sc = ((float)Math.sqrt(x) / sc - 1.0f) * alpha + 1.0f;
           pd *= sc;
           if (uniform) {
               sa *= sc;
           }
       }
       Bone boneTmp = bone;
       boneTmp.updateWorldTransform(boneTmp.ax, bone.ay, bone.arotation + rotationIK * alpha, pd, sa, bone.ashearX, bone.ashearY);
   }

}
