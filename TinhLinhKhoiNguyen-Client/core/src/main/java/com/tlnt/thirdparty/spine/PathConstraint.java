package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.tlnt.thirdparty.spine.attachment.PathAttachment;
import com.tlnt.thirdparty.spine.utils.SpineUtils;
import java.util.Arrays;

public final class PathConstraint implements Updatable {
   public final PathConstraintData data;
   public final Array<Bone> bones;
   public Slot target;
   public float position;
   public float spacing;
   public float mixRotate;
   public float mixX;
   public float mixY;
   public boolean active;
   public final FloatArray spaces = new FloatArray();
   public final FloatArray positions = new FloatArray();
   public final FloatArray world = new FloatArray();
   public final FloatArray curves = new FloatArray();
   public final FloatArray lengths = new FloatArray();
   public final float[] segments;

   public PathConstraint(PathConstraintData data, Skeleton skeleton) {
      this.segments = new float[10];
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
         this.bones = new Array<>(data.bones.size);

         for (BoneData boneData : data.bones) {
            this.bones.add(skeleton.bones.get(boneData.index));
         }

         this.target = skeleton.slots.get(data.target.index);
         this.position = data.position;
         this.spacing = data.spacing;
         this.mixRotate = data.mixRotate;
         this.mixX = data.mixX;
         this.mixY = data.mixY;
      }
   }

   @Override
   public final void update(int physics) {
      // [recovered by hand from reference bytecode; Vineflower/CFR failed on this method]
      com.tlnt.thirdparty.spine.attachment.Attachment attachment = this.target.attachment;
      if (!(attachment instanceof PathAttachment)) {
         return;
      }

      float mixRotate = this.mixRotate;
      float mixX = this.mixX;
      float mixY = this.mixY;
      if (mixRotate == 0.0F && mixX == 0.0F && mixY == 0.0F) {
         return;
      }

      PathConstraintData data = this.data;
      boolean tangents = data.rotateMode == PathConstraintData.RotateMode.TANGENT;
      boolean scale = data.rotateMode == PathConstraintData.RotateMode.CHAIN_SCALE;
      int boneCount = this.bones.size;
      int spacesCount = tangents ? boneCount : boneCount + 1;
      Object[] bones = this.bones.items;
      float[] spaces = this.spaces.setSize(spacesCount);
      float[] lengths = scale ? this.lengths.setSize(boneCount) : null;
      float spacing = this.spacing;
      switch (data.spacingMode) {
         case PathConstraintData.SpacingMode.PERCENT:
            if (scale) {
               int i = 0;

               for (int n = spacesCount - 1; i < n; i++) {
                  Bone bone = (Bone)bones[i];
                  float setupLength = bone.data.length;
                  float x = setupLength * bone.a;
                  float y = setupLength * bone.c;
                  lengths[i] = (float)Math.sqrt(x * x + y * y);
               }
            }

            Arrays.fill(spaces, 1, spacesCount, spacing);
            break;
         case PathConstraintData.SpacingMode.PROPORTIONAL: {
            float sum = 0.0F;
            int i = 0;
            int n = spacesCount - 1;

            while (i < n) {
               Bone bone = (Bone)bones[i];
               float setupLength = bone.data.length;
               if (setupLength < 1.0E-5F) {
                  if (scale) {
                     lengths[i] = 0.0F;
                  }

                  spaces[++i] = spacing;
               } else {
                  float x = setupLength * bone.a;
                  float y = setupLength * bone.c;
                  float length = (float)Math.sqrt(x * x + y * y);
                  if (scale) {
                     lengths[i] = length;
                  }

                  spaces[++i] = length;
                  sum += length;
               }
            }

            if (sum > 0.0F) {
               sum = spacesCount / sum * spacing;

               for (i = 1; i < spacesCount; i++) {
                  spaces[i] *= sum;
               }
            }
            break;
         }
         default: {
            boolean lengthSpacing = data.spacingMode == PathConstraintData.SpacingMode.LENGTH;
            int i = 0;
            int n = spacesCount - 1;

            while (i < n) {
               Bone bone = (Bone)bones[i];
               float setupLength = bone.data.length;
               if (setupLength < 1.0E-5F) {
                  if (scale) {
                     lengths[i] = 0.0F;
                  }

                  spaces[++i] = spacing;
               } else {
                  float x = setupLength * bone.a;
                  float y = setupLength * bone.c;
                  float length = (float)Math.sqrt(x * x + y * y);
                  if (scale) {
                     lengths[i] = length;
                  }

                  spaces[++i] = (lengthSpacing ? setupLength + spacing : spacing) * length / setupLength;
               }
            }
         }
      }

      // --- inlined computeWorldPositions(path, spacesCount, tangents) ---
      float[] positions;
      computeWorldPositions:
      {
         boolean cwTangents = tangents;
         int cwSpacesCount = spacesCount;
         PathAttachment path = (PathAttachment)attachment;
         Slot target = this.target;
         float position = this.position;
         float[] cwSpaces = this.spaces.items;
         float[] out = this.positions.setSize(cwSpacesCount * 3 + 2);
         float[] world;
         boolean closed = path.getClosed();
         int verticesLength = path.getWorldVerticesLength();
         int curveCount = verticesLength / 6;
         int prevCurve = -1;
         if (!path.getConstantSpeed()) {
            float[] pathLengths = path.getLengths();
            curveCount -= closed ? 1 : 2;
            float pathLength = pathLengths[curveCount];
            if (this.data.positionMode == PathConstraintData.PositionMode.PERCENT) {
               position *= pathLength;
            }

            float multiplier;
            switch (this.data.spacingMode) {
               case PathConstraintData.SpacingMode.PERCENT:
                  multiplier = pathLength;
                  break;
               case PathConstraintData.SpacingMode.PROPORTIONAL:
                  multiplier = pathLength / cwSpacesCount;
                  break;
               default:
                  multiplier = 1.0F;
            }

            world = this.world.setSize(8);
            for (int i = 0, o = 0, curve = 0; i < cwSpacesCount; i++, o += 3) {
               float space = cwSpaces[i] * multiplier;
               position += space;
               float p = position;
               if (closed) {
                  p %= pathLength;
                  if (p < 0.0F) {
                     p += pathLength;
                  }

                  curve = 0;
               } else if (p < 0.0F) {
                  if (prevCurve != -2) {
                     prevCurve = -2;
                     path.computeWorldVertices(target, 2, 4, world, 0, 2);
                  }

                  addBeforePosition(p, world, 0, out, o);
                  continue;
               } else if (p > pathLength) {
                  if (prevCurve != -3) {
                     prevCurve = -3;
                     path.computeWorldVertices(target, verticesLength - 6, 4, world, 0, 2);
                  }

                  addAfterPosition(p - pathLength, world, 0, out, o);
                  continue;
               }

               {
                  while (true) {
                     float length = pathLengths[curve];
                     if (!(p > length)) {
                        if (curve == 0) {
                           p /= length;
                        } else {
                           float prev = pathLengths[curve - 1];
                           p = (p - prev) / (length - prev);
                        }
                        break;
                     }

                     curve++;
                  }

                  if (curve != prevCurve) {
                     prevCurve = curve;
                     if (closed && curve == curveCount) {
                        path.computeWorldVertices(target, verticesLength - 4, 4, world, 0, 2);
                        path.computeWorldVertices(target, 0, 4, world, 4, 2);
                     } else {
                        path.computeWorldVertices(target, curve * 6 + 2, 8, world, 0, 2);
                     }
                  }

                  addCurvePosition(
                     p,
                     world[0],
                     world[1],
                     world[2],
                     world[3],
                     world[4],
                     world[5],
                     world[6],
                     world[7],
                     out,
                     o,
                     cwTangents || i > 0 && space < 1.0E-5F
                  );
               }
            }

            positions = out;
            break computeWorldPositions;
         }

         if (closed) {
            verticesLength += 2;
            world = this.world.setSize(verticesLength);
            path.computeWorldVertices(target, 2, verticesLength - 4, world, 0, 2);
            path.computeWorldVertices(target, 0, 2, world, verticesLength - 4, 2);
            world[verticesLength - 2] = world[0];
            world[verticesLength - 1] = world[1];
         } else {
            curveCount--;
            verticesLength -= 4;
            world = this.world.setSize(verticesLength);
            path.computeWorldVertices(target, 2, verticesLength, world, 0, 2);
         }

         float[] curves = this.curves.setSize(curveCount);
         float pathLength = 0.0F;
         float x1 = world[0];
         float y1 = world[1];
         float cx1 = 0.0F;
         float cy1 = 0.0F;
         float cx2 = 0.0F;
         float cy2 = 0.0F;
         float x2 = 0.0F;
         float y2 = 0.0F;
         int ci = 0;

         for (int w = 2; ci < curveCount; w += 6) {
            cx1 = world[w];
            cy1 = world[w + 1];
            cx2 = world[w + 2];
            cy2 = world[w + 3];
            x2 = world[w + 4];
            y2 = world[w + 5];
            float tmpx = (x1 - cx1 * 2.0F + cx2) * 0.1875F;
            float tmpy = (y1 - cy1 * 2.0F + cy2) * 0.1875F;
            float dddfx = ((cx1 - cx2) * 3.0F - x1 + x2) * 0.09375F;
            float dddfy = ((cy1 - cy2) * 3.0F - y1 + y2) * 0.09375F;
            float ddfx = tmpx * 2.0F + dddfx;
            float ddfy = tmpy * 2.0F + dddfy;
            float dfx = (cx1 - x1) * 0.75F + tmpx + dddfx * 0.16666667F;
            float dfy = (cy1 - y1) * 0.75F + tmpy + dddfy * 0.16666667F;
            pathLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
            dfx += ddfx;
            dfy += ddfy;
            ddfx += dddfx;
            ddfy += dddfy;
            pathLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
            dfx += ddfx;
            dfy += ddfy;
            pathLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
            dfx += ddfx + dddfx;
            dfy += ddfy + dddfy;
            pathLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
            curves[ci] = pathLength;
            x1 = x2;
            y1 = y2;
            ci++;
         }

         if (this.data.positionMode == PathConstraintData.PositionMode.PERCENT) {
            position *= pathLength;
         }

         float multiplier;
         switch (this.data.spacingMode) {
            case PathConstraintData.SpacingMode.PERCENT:
               multiplier = pathLength;
               break;
            case PathConstraintData.SpacingMode.PROPORTIONAL:
               multiplier = pathLength / cwSpacesCount;
               break;
            default:
               multiplier = 1.0F;
         }

         float[] segments = this.segments;
         float curveLength = 0.0F;
         for (int i = 0, o = 0, curve = 0, segment = 0; i < cwSpacesCount; i++, o += 3) {
            float space = cwSpaces[i] * multiplier;
            position += space;
            float p = position;
            if (closed) {
               p %= pathLength;
               if (p < 0.0F) {
                  p += pathLength;
               }

               curve = 0;
            } else if (p < 0.0F) {
               addBeforePosition(p, world, 0, out, o);
               continue;
            } else if (p > pathLength) {
               addAfterPosition(p - pathLength, world, verticesLength - 4, out, o);
               continue;
            }

            {
               while (true) {
                  float length = curves[curve];
                  if (!(p > length)) {
                     if (curve == 0) {
                        p /= length;
                     } else {
                        float prev = curves[curve - 1];
                        p = (p - prev) / (length - prev);
                     }
                     break;
                  }

                  curve++;
               }

               if (curve != prevCurve) {
                  prevCurve = curve;
                  int ii = curve * 6;
                  x1 = world[ii];
                  y1 = world[ii + 1];
                  cx1 = world[ii + 2];
                  cy1 = world[ii + 3];
                  cx2 = world[ii + 4];
                  cy2 = world[ii + 5];
                  x2 = world[ii + 6];
                  y2 = world[ii + 7];
                  float tmpx = (x1 - cx1 * 2.0F + cx2) * 0.03F;
                  float tmpy = (y1 - cy1 * 2.0F + cy2) * 0.03F;
                  float dddfx = ((cx1 - cx2) * 3.0F - x1 + x2) * 0.006F;
                  float dddfy = ((cy1 - cy2) * 3.0F - y1 + y2) * 0.006F;
                  float ddfx = tmpx * 2.0F + dddfx;
                  float ddfy = tmpy * 2.0F + dddfy;
                  float dfx = (cx1 - x1) * 0.3F + tmpx + dddfx * 0.16666667F;
                  float dfy = (cy1 - y1) * 0.3F + tmpy + dddfy * 0.16666667F;
                  curveLength = (float)Math.sqrt(dfx * dfx + dfy * dfy);
                  segments[0] = curveLength;

                  for (ii = 1; ii < 8; ii++) {
                     dfx += ddfx;
                     dfy += ddfy;
                     ddfx += dddfx;
                     ddfy += dddfy;
                     curveLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
                     segments[ii] = curveLength;
                  }

                  dfx += ddfx;
                  dfy += ddfy;
                  curveLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
                  segments[8] = curveLength;
                  dfx += ddfx + dddfx;
                  dfy += ddfy + dddfy;
                  curveLength += (float)Math.sqrt(dfx * dfx + dfy * dfy);
                  segments[9] = curveLength;
                  segment = 0;
               }

               p *= curveLength;

               while (true) {
                  float length = segments[segment];
                  if (!(p > length)) {
                     if (segment == 0) {
                        p /= length;
                     } else {
                        float prev = segments[segment - 1];
                        p = segment + (p - prev) / (length - prev);
                     }
                     break;
                  }

                  segment++;
               }

               addCurvePosition(p * 0.1F, x1, y1, cx1, cy1, cx2, cy2, x2, y2, out, o, cwTangents || i > 0 && space < 1.0E-5F);
            }
         }

         positions = out;
      }

      float boneX = positions[0];
      float boneY = positions[1];
      float offsetRotation = data.offsetRotation;
      boolean tip;
      if (offsetRotation == 0.0F) {
         tip = data.rotateMode == PathConstraintData.RotateMode.CHAIN;
      } else {
         tip = false;
         Bone pb = this.target.bone;
         offsetRotation *= pb.a * pb.d - pb.b * pb.c > 0.0F ? 0.017453292F : -0.017453292F;
      }

      int i = 0;

      for (int p = 3; i < boneCount; p += 3) {
         Bone bone = (Bone)bones[i];
         bone.worldX = bone.worldX + (boneX - bone.worldX) * mixX;
         bone.worldY = bone.worldY + (boneY - bone.worldY) * mixY;
         float x = positions[p];
         float y = positions[p + 1];
         float dx = x - boneX;
         float dy = y - boneY;
         if (scale) {
            float length = lengths[i];
            if (length >= 1.0E-5F) {
               float s = ((float)Math.sqrt(dx * dx + dy * dy) / length - 1.0F) * mixRotate + 1.0F;
               bone.a *= s;
               bone.c *= s;
            }
         }

         boneX = x;
         boneY = y;
         if (mixRotate > 0.0F) {
            float a = bone.a;
            float b = bone.b;
            float c = bone.c;
            float d = bone.d;
            float r;
            if (tangents) {
               r = positions[p - 1];
            } else if (spaces[i + 1] < 1.0E-5F) {
               r = positions[p + 2];
            } else {
               r = SpineUtils.atan2(dy, dx);
            }

            r -= SpineUtils.atan2(c, a);
            if (tip) {
               float cos = (float)Math.cos(r);
               float sin = (float)Math.sin(r);
               float length = bone.data.length;
               boneX += (length * (cos * a - sin * c) - dx) * mixRotate;
               boneY += (length * (sin * a + cos * c) - dy) * mixRotate;
            } else {
               r += offsetRotation;
            }

            if (r > 3.1415927F) {
               r -= 6.2831855F;
            } else if (r < -3.1415927F) {
               r += 6.2831855F;
            }

            r *= mixRotate;
            float cos = (float)Math.cos(r);
            float sin = (float)Math.sin(r);
            bone.a = cos * a - sin * c;
            bone.b = cos * b - sin * d;
            bone.c = sin * a + cos * c;
            bone.d = sin * b + cos * d;
         }

         bone.updateAppliedTransform();
         i++;
      }
   }

   public static void addBeforePosition(float p, float[] temp, int i, float[] out, int o) {
      float x1 = temp[0];
      float y1 = temp[1];
      float dx = temp[2] - x1;
      float r = SpineUtils.atan2(temp[3] - y1, dx);
      out[o] = x1 + p * (float)Math.cos((double)r);
      out[o + 1] = y1 + p * (float)Math.sin((double)r);
      out[o + 2] = r;
   }

   public static void addAfterPosition(float p, float[] temp, int i, float[] out, int o) {
      float x1 = temp[i + 2];
      float y1 = temp[i + 3];
      float dx = x1 - temp[i];
      float dy;
      float r = SpineUtils.atan2(dy = y1 - temp[i + 1], dx);
      out[o] = x1 + p * (float)Math.cos((double)r);
      out[o + 1] = y1 + p * (float)Math.sin((double)r);
      out[o + 2] = r;
   }

   public static void addCurvePosition(
      float p, float x1, float y1, float cx1, float cy1, float cx2, float cy2, float x2, float y2, float[] out, int o, boolean tangents
   ) {
      if (!(p < 1.0E-5F) && !Float.isNaN(p)) {
         float tt;
         float ttt = (tt = p * p) * p;
         float u;
         float uu;
         float uuu = (uu = (u = 1.0F - p) * u) * u;
         float ut;
         float ut3 = (ut = u * p) * 3.0F;
         u *= ut3;
         ut3 *= p;
         x2 = x1 * uuu + cx1 * u + cx2 * ut3 + x2 * ttt;
         y2 = y1 * uuu + cy1 * u + cy2 * ut3 + y2 * ttt;
         out[o] = x2;
         out[o + 1] = y2;
         if (tangents) {
            if (p < 0.001F) {
               out[o + 2] = SpineUtils.atan2(cy1 - y1, cx1 - x1);
               return;
            }

            out[o + 2] = SpineUtils.atan2(
               y2 - (y1 * uu + cy1 * ut * 2.0F + cy2 * tt), x2 - (x1 * uu + cx1 * ut * 2.0F + cx2 * tt)
            );
         }
      } else {
         out[o] = x1;
         out[o + 1] = y1;
         out[o + 2] = SpineUtils.atan2(cy1 - y1, cx1 - x1);
      }
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

}
