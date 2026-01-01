package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.tlnt.thirdparty.spine.utils.SpineUtils;

public final class TransformConstraint implements Updatable {
   public final TransformConstraintData data;
   public final Array<Bone> bones;
   public Bone target;
   public float mixRotate;
   public float mixX;
   public float mixY;
   public float mixScaleX;
   public float mixScaleY;
   public float mixShearY;
   public boolean active;
   public Vector2 temp = new Vector2();

   public TransformConstraint(TransformConstraintData data, Skeleton skeleton) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
         this.bones = new Array<>(data.bones.size);

         for (BoneData boneData : data.bones) {
            this.bones.add(skeleton.bones.get(boneData.index));
         }

         this.target = skeleton.bones.get(data.target.index);
         this.mixRotate = data.mixRotate;
         this.mixX = data.mixX;
         this.mixY = data.mixY;
         this.mixScaleX = data.mixScaleX;
         this.mixScaleY = data.mixScaleY;
         this.mixShearY = data.mixShearY;
      }
   }

   @Override
   public final void update(int physics) {
      if (this.mixRotate != 0.0F || this.mixX != 0.0F || this.mixY != 0.0F || this.mixScaleX != 0.0F || this.mixScaleY != 0.0F || this.mixShearY != 0.0F) {
         if (this.data.local) {
            if (!this.data.relative) {
               TransformConstraint selfAL = this;
               float mixRotateAL = this.mixRotate;
               float mixXAL = this.mixX;
               float mixYAL = this.mixY;
               float mixScaleXAL = this.mixScaleX;
               float mixScaleYAL = this.mixScaleY;
               float mixShearYAL = this.mixShearY;
               Bone targetAL = this.target;
               Object[] bonesAL = this.bones.items;
               int iAL = 0;

               for (int nAL = this.bones.size; iAL < nAL; iAL++) {
                  Bone boneAL;
                  float rotationAL = (boneAL = (Bone)bonesAL[iAL]).arotation;
                  if (mixRotateAL != 0.0F) {
                     rotationAL += (targetAL.arotation - rotationAL + selfAL.data.offsetRotation) * mixRotateAL;
                  }

                  float xAL = boneAL.ax;
                  float yAL = boneAL.ay;
                  xAL += (targetAL.ax - xAL + selfAL.data.offsetX) * mixXAL;
                  yAL += (targetAL.ay - yAL + selfAL.data.offsetY) * mixYAL;
                  float scaleXAL = boneAL.ascaleX;
                  float scaleYAL = boneAL.ascaleY;
                  if (mixScaleXAL != 0.0F && scaleXAL != 0.0F) {
                     scaleXAL = (scaleXAL + (targetAL.ascaleX - scaleXAL + selfAL.data.offsetScaleX) * mixScaleXAL) / scaleXAL;
                  }

                  if (mixScaleYAL != 0.0F && scaleYAL != 0.0F) {
                     scaleYAL = (scaleYAL + (targetAL.ascaleY - scaleYAL + selfAL.data.offsetScaleY) * mixScaleYAL) / scaleYAL;
                  }

                  float shearYAL = boneAL.ashearY;
                  if (mixShearYAL != 0.0F) {
                     shearYAL += (targetAL.ashearY - shearYAL + selfAL.data.offsetShearY) * mixShearYAL;
                  }

                  boneAL.updateWorldTransform(xAL, yAL, rotationAL, scaleXAL, scaleYAL, boneAL.ashearX, shearYAL);
               }
            } else {
               TransformConstraint selfRL = this;
               float mixRotateRL = this.mixRotate;
               float mixXRL = this.mixX;
               float mixYRL = this.mixY;
               float mixScaleXRL = this.mixScaleX;
               float mixScaleYRL = this.mixScaleY;
               float mixShearYRL = this.mixShearY;
               Bone targetRL = this.target;
               Object[] bonesRL = this.bones.items;
               int iRL = 0;

               for (int nRL = this.bones.size; iRL < nRL; iRL++) {
                  Bone boneRL;
                  float rotationRL = (boneRL = (Bone)bonesRL[iRL]).arotation + (targetRL.arotation + selfRL.data.offsetRotation) * mixRotateRL;
                  float xRL = boneRL.ax + (targetRL.ax + selfRL.data.offsetX) * mixXRL;
                  float yRL = boneRL.ay + (targetRL.ay + selfRL.data.offsetY) * mixYRL;
                  float scaleXRL = boneRL.ascaleX * ((targetRL.ascaleX - 1.0F + selfRL.data.offsetScaleX) * mixScaleXRL + 1.0F);
                  float scaleYRL = boneRL.ascaleY * ((targetRL.ascaleY - 1.0F + selfRL.data.offsetScaleY) * mixScaleYRL + 1.0F);
                  float shearYRL = boneRL.ashearY + (targetRL.ashearY + selfRL.data.offsetShearY) * mixShearYRL;
                  boneRL.updateWorldTransform(xRL, yRL, rotationRL, scaleXRL, scaleYRL, boneRL.ashearX, shearYRL);
               }
            }
         } else if (this.data.relative) {
            TransformConstraint selfRW = this;
            float mixRotateRW = this.mixRotate;
            float mixXRW = this.mixX;
            float mixYRW = this.mixY;
            float mixScaleXRW = this.mixScaleX;
            float mixScaleYRW = this.mixScaleY;
            float mixShearYRW = this.mixShearY;
            int translateFlagRW;
            if (mixXRW == 0.0F && mixYRW == 0.0F) {
               translateFlagRW = 0;
            } else {
               translateFlagRW = 1;
            }

            int translateRW = translateFlagRW;
            Bone targetRW = this.target;
            float taRW = this.target.a;
            float tbRW = targetRW.b;
            float tcRW = targetRW.c;
            float tdRW = targetRW.d;
            float degRadReflectRW = taRW * tdRW - tbRW * tcRW > 0.0F ? (float) (Math.PI / 180.0) : (float) (-Math.PI / 180.0);
            float offsetRotationRW = this.data.offsetRotation * degRadReflectRW;
            float offsetShearYRW = this.data.offsetShearY * degRadReflectRW;
            Object[] bonesRW = this.bones.items;
            int iRW = 0;

            for (int nRW = this.bones.size; iRW < nRW; iRW++) {
               Bone boneRW = (Bone)bonesRW[iRW];
               if (mixRotateRW != 0.0F) {
                  float aRW = boneRW.a;
                  float bRW = boneRW.b;
                  float cRW = boneRW.c;
                  float dRW = boneRW.d;
                  float rRW;
                  if ((rRW = SpineUtils.atan2(tcRW, taRW) + offsetRotationRW) > (float) Math.PI) {
                     rRW -= (float) (Math.PI * 2);
                  } else if (rRW < (float) -Math.PI) {
                     rRW += (float) (Math.PI * 2);
                  }

                  float rMixRW;
                  float cosRW = (float)Math.cos((double)(rMixRW = rRW * mixRotateRW));
                  rRW = (float)Math.sin((double)rMixRW);
                  boneRW.a = cosRW * aRW - rRW * cRW;
                  boneRW.b = cosRW * bRW - rRW * dRW;
                  boneRW.c = rRW * aRW + cosRW * cRW;
                  boneRW.d = rRW * bRW + cosRW * dRW;
               }

               if (translateRW != 0) {
                  Vector2 tempRW = selfRW.temp;
                  targetRW.localToWorld(tempRW.set(selfRW.data.offsetX, selfRW.data.offsetY));
                  boneRW.worldX = boneRW.worldX + tempRW.x * mixXRW;
                  boneRW.worldY = boneRW.worldY + tempRW.y * mixYRW;
               }

               if (mixScaleXRW != 0.0F) {
                  float sxRW = ((float)Math.sqrt((double)(taRW * taRW + tcRW * tcRW)) - 1.0F + selfRW.data.offsetScaleX) * mixScaleXRW + 1.0F;
                  boneRW.a *= sxRW;
                  boneRW.c *= sxRW;
               }

               if (mixScaleYRW != 0.0F) {
                  float syRW = ((float)Math.sqrt((double)(tbRW * tbRW + tdRW * tdRW)) - 1.0F + selfRW.data.offsetScaleY) * mixScaleYRW + 1.0F;
                  boneRW.b *= syRW;
                  boneRW.d *= syRW;
               }

               if (mixShearYRW > 0.0F) {
                  float shearRRW;
                  if ((shearRRW = SpineUtils.atan2(tdRW, tbRW) - SpineUtils.atan2(tcRW, taRW)) > (float) Math.PI) {
                     shearRRW -= (float) (Math.PI * 2);
                  } else if (shearRRW < (float) -Math.PI) {
                     shearRRW += (float) (Math.PI * 2);
                  }

                  float shearBRW = boneRW.b;
                  float shearDRW = boneRW.d;
                  shearRRW = SpineUtils.atan2(boneRW.d, shearBRW) + (shearRRW - (float) (Math.PI / 2) + offsetShearYRW) * mixShearYRW;
                  float shearSRW = (float)Math.sqrt((double)(shearBRW * shearBRW + shearDRW * shearDRW));
                  boneRW.b = (float)Math.cos((double)shearRRW) * shearSRW;
                  boneRW.d = (float)Math.sin((double)shearRRW) * shearSRW;
               }

               boneRW.updateAppliedTransform();
            }
         } else {
            TransformConstraint selfAW = this;
            float mixRotateAW = this.mixRotate;
            float mixXAW = this.mixX;
            float mixYAW = this.mixY;
            float mixScaleXAW = this.mixScaleX;
            float mixScaleYAW = this.mixScaleY;
            float mixShearYAW = this.mixShearY;
            int translateFlagAW;
            if (mixXAW == 0.0F && mixYAW == 0.0F) {
               translateFlagAW = 0;
            } else {
               translateFlagAW = 1;
            }

            int translateAW = translateFlagAW;
            Bone targetAW = this.target;
            float taAW = this.target.a;
            float tbAW = targetAW.b;
            float tcAW = targetAW.c;
            float tdAW = targetAW.d;
            float degRadReflectAW = taAW * tdAW - tbAW * tcAW > 0.0F ? (float) (Math.PI / 180.0) : (float) (-Math.PI / 180.0);
            float offsetRotationAW = this.data.offsetRotation * degRadReflectAW;
            float offsetShearYAW = this.data.offsetShearY * degRadReflectAW;
            Object[] bonesAW = this.bones.items;
            int iAW = 0;

            for (int nAW = this.bones.size; iAW < nAW; iAW++) {
               Bone boneAW = (Bone)bonesAW[iAW];
               if (mixRotateAW != 0.0F) {
                  float aAW = boneAW.a;
                  float bAW = boneAW.b;
                  float cAW = boneAW.c;
                  float dAW = boneAW.d;
                  float rAW;
                  if ((rAW = SpineUtils.atan2(tcAW, taAW) - SpineUtils.atan2(cAW, aAW) + offsetRotationAW) > (float) Math.PI) {
                     rAW -= (float) (Math.PI * 2);
                  } else if (rAW < (float) -Math.PI) {
                     rAW += (float) (Math.PI * 2);
                  }

                  float rMixAW;
                  float cosAW = (float)Math.cos((double)(rMixAW = rAW * mixRotateAW));
                  rAW = (float)Math.sin((double)rMixAW);
                  boneAW.a = cosAW * aAW - rAW * cAW;
                  boneAW.b = cosAW * bAW - rAW * dAW;
                  boneAW.c = rAW * aAW + cosAW * cAW;
                  boneAW.d = rAW * bAW + cosAW * dAW;
               }

               if (translateAW != 0) {
                  Vector2 tempAW = selfAW.temp;
                  targetAW.localToWorld(tempAW.set(selfAW.data.offsetX, selfAW.data.offsetY));
                  boneAW.worldX = boneAW.worldX + (tempAW.x - boneAW.worldX) * mixXAW;
                  boneAW.worldY = boneAW.worldY + (tempAW.y - boneAW.worldY) * mixYAW;
               }

               if (mixScaleXAW != 0.0F) {
                  float sxAW;
                  if ((sxAW = (float)Math.sqrt((double)(boneAW.a * boneAW.a + boneAW.c * boneAW.c))) != 0.0F) {
                     sxAW = (sxAW + ((float)Math.sqrt((double)(taAW * taAW + tcAW * tcAW)) - sxAW + selfAW.data.offsetScaleX) * mixScaleXAW) / sxAW;
                  }

                  boneAW.a *= sxAW;
                  boneAW.c *= sxAW;
               }

               if (mixScaleYAW != 0.0F) {
                  float syAW;
                  if ((syAW = (float)Math.sqrt((double)(boneAW.b * boneAW.b + boneAW.d * boneAW.d))) != 0.0F) {
                     syAW = (syAW + ((float)Math.sqrt((double)(tbAW * tbAW + tdAW * tdAW)) - syAW + selfAW.data.offsetScaleY) * mixScaleYAW) / syAW;
                  }

                  boneAW.b *= syAW;
                  boneAW.d *= syAW;
               }

               if (mixShearYAW > 0.0F) {
                  float shearBAW = boneAW.b;
                  float shearDAW = boneAW.d;
                  float byAW = SpineUtils.atan2(boneAW.d, shearBAW);
                  float shearRAW;
                  if ((shearRAW = SpineUtils.atan2(tdAW, tbAW) - SpineUtils.atan2(tcAW, taAW) - (byAW - SpineUtils.atan2(boneAW.c, boneAW.a)))
                     > (float) Math.PI) {
                     shearRAW -= (float) (Math.PI * 2);
                  } else if (shearRAW < (float) -Math.PI) {
                     shearRAW += (float) (Math.PI * 2);
                  }

                  shearRAW = byAW + (shearRAW + offsetShearYAW) * mixShearYAW;
                  float shearSAW = (float)Math.sqrt((double)(shearBAW * shearBAW + shearDAW * shearDAW));
                  boneAW.b = (float)Math.cos((double)shearRAW) * shearSAW;
                  boneAW.d = (float)Math.sin((double)shearRAW) * shearSAW;
               }

               boneAW.updateAppliedTransform();
            }
         }
      }
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

}
