package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.ObjectSet;
import com.tlnt.thirdparty.spine.attachment.Attachment;
import com.tlnt.thirdparty.spine.attachment.HasTextureRegion;
import com.tlnt.thirdparty.spine.attachment.VertexAttachment;
import com.tlnt.thirdparty.spine.utils.SpineUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.tlnt.thirdparty.spine.Bone;
import com.tlnt.thirdparty.spine.BoneData;
import com.tlnt.thirdparty.spine.Event;
import com.tlnt.thirdparty.spine.IkConstraint;
import com.tlnt.thirdparty.spine.PathConstraint;
import com.tlnt.thirdparty.spine.PathConstraintData;
import com.tlnt.thirdparty.spine.PhysicsConstraint;
import com.tlnt.thirdparty.spine.PhysicsConstraintData;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.Slot;
import com.tlnt.thirdparty.spine.TransformConstraint;
import com.tlnt.thirdparty.spine.TransformConstraintData;
import com.tlnt.thirdparty.spine.attachment.Sequence;

public final class Animation {
   public final String name;
   public Array<Animation.Timeline> timelines;
   public ObjectSet<String> timelineIds;
   public float duration;

   public Animation(String name, Array<Animation.Timeline> timelines, float duration) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else {
         this.name = name;
         this.duration = duration;
         this.timelineIds = new ObjectSet<>(timelines.size);
         Animation animation = this;
         if (timelines == null) {
            throw new IllegalArgumentException("timelines cannot be null.");
         } else {
            this.timelines = timelines;
            int n = timelines.size;
            this.timelineIds.clear(n);
            Object[] items = timelines.items;

            for (int i = 0; i < n; i++) {
               animation.timelineIds.addAll(((Animation.Timeline)items[i]).getPropertyIds());
            }
         }
      }
   }

   public final boolean hasTimeline(String[] propertyIds) {
      String[] ids;
      int n = (ids = propertyIds).length;

      for (int i = 0; i < n; i++) {
         String id = ids[i];
         if (this.timelineIds.contains(id)) {
            return true;
         }
      }

      return false;
   }

   public final float getDuration() {
      return this.duration;
   }

   public final String getName() {
      return this.name;
   }

   @Override
   public final String toString() {
      return this.name;
   }

   public static class AlphaTimeline extends Animation.CurveTimeline1 {
      public int slotIndex;

      public AlphaTimeline(int frameCount, int bezierCount, int slotIndex) {
         super(frameCount, bezierCount, Animation.Property.ALPHA - 1 + "|" + slotIndex);
         this.slotIndex = slotIndex;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          Slot slot = skeleton.slots.get(this.slotIndex);
          if (!slot.bone.active) {
              return;
          }
          float[] frames = this.frames;
          Color color = slot.color;
          if (time < frames[0]) {
              Color setup = slot.data.color;
              switch (blend) {
                  case MixBlend.SETUP: {
                      color.a = setup.a;
                      return;
                  }
                  case MixBlend.FIRST: {
                      color.a += (setup.a - color.a) * alpha;
                  }
              }
              return;
          }
          float a = this.getCurveValue(time);
          if (alpha == 1.0f) {
              color.a = a;
              return;
          }
          if (blend == MixBlend.SETUP) {
              color.a = slot.data.color.a;
          }
          color.a += (a - color.a) * alpha;
      }
   }

   public static class AttachmentTimeline extends Animation.Timeline {
      public final int PROPERTY_IDS;
      public final String[] attachmentNames;

      public AttachmentTimeline(int frameCount, int slotIndex) {
         super(frameCount, new String[]{Animation.Property.ATTACHMENT - 1 + "|" + slotIndex});
         this.PROPERTY_IDS = slotIndex;
         this.attachmentNames = new String[frameCount];
      }

      @Override
      public final int getFrameCount() {
         return super.frames.length;
      }

      public final void setFrame(int frame, float time, String attachmentName) {
         super.frames[frame] = time;
         this.attachmentNames[frame] = attachmentName;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Slot slot;
         if ((slot = skeleton.slots.get(this.PROPERTY_IDS)).bone.active) {
            if (direction == Animation.MixDirection.OUT) {
               if (blend == Animation.MixBlend.SETUP) {
                  this.setAttachment(skeleton, slot, slot.data.attachmentName);
               }
            } else if (!(time < super.frames[0])) {
               this.setAttachment(skeleton, slot, this.attachmentNames[search(super.frames, time)]);
            } else {
               if (blend == Animation.MixBlend.SETUP || blend == Animation.MixBlend.FIRST) {
                  this.setAttachment(skeleton, slot, slot.data.attachmentName);
               }
            }
         }
      }

      public void setAttachment(Skeleton skeleton, Slot slot, String attachmentName) {
         slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(this.PROPERTY_IDS, attachmentName));
      }
   }

   public abstract static class CurveTimeline extends Animation.Timeline {
      public float[] PROPERTY_IDS;

      public CurveTimeline(int frameCount, int bezierCount, String... propertyIds) {
         super(frameCount, propertyIds);
         this.PROPERTY_IDS = new float[frameCount + bezierCount * 18];
         this.PROPERTY_IDS[frameCount - 1] = 1.0F;
      }

      public void setBezier(int bezier, int frame, int value, float time1, float value1, float cx1, float cy1, float cx2, float cy2, float time2, float value2) {
         float[] curves = this.PROPERTY_IDS;
         bezier = this.getFrameCount() + bezier * 18;
         if (value == 0) {
            curves[frame] = (float)(bezier + 2);
         }

         float tmpx = (time1 - cx1 * 2.0F + cx2) * 0.03F;
         float tmpy = (value1 - cy1 * 2.0F + cy2) * 0.03F;
         cx2 = ((cx1 - cx2) * 3.0F - time1 + time2) * 0.006F;
         cy2 = ((cy1 - cy2) * 3.0F - value1 + value2) * 0.006F;
         time2 = tmpx * 2.0F + cx2;
         value2 = tmpy * 2.0F + cy2;
         float dx = (cx1 - time1) * 0.3F + tmpx + cx2 * 0.16666667F;
         float dy = (cy1 - value1) * 0.3F + tmpy + cy2 * 0.16666667F;
         time1 += dx;
         value1 += dy;

         for (int n = bezier + 18; bezier < n; bezier += 2) {
            curves[bezier] = time1;
            curves[bezier + 1] = value1;
            dx += time2;
            dy += value2;
            time2 += cx2;
            value2 += cy2;
            time1 += dx;
            value1 += dy;
         }
      }

      public final float getBezierValue(float time, int frameIndex, int valueOffset, int i) {
         float[] curves = this.PROPERTY_IDS;
         if (this.PROPERTY_IDS[i] > time) {
            float x = super.frames[frameIndex];
            float y;
            return (y = super.frames[frameIndex + valueOffset]) + (time - x) / (curves[i] - x) * (curves[i + 1] - y);
         } else {
            int n = i + 18;

            for (int j = i + 2; j < n; j += 2) {
               if (curves[j] >= time) {
                  float bx = curves[j - 2];
                  float by;
                  return (by = curves[j - 1])
                     + (time - bx) / (curves[j] - bx) * (curves[j + 1] - by);
               }
            }

            frameIndex += this.getFrameEntries();
            float endX = curves[n - 2];
            float endY;
            return (endY = curves[n - 1]) + (time - endX) / (super.frames[frameIndex] - endX) * (super.frames[frameIndex + valueOffset] - endY);
         }
      }
   }

   public abstract static class CurveTimeline1 extends Animation.CurveTimeline {
      public CurveTimeline1(int frameCount, int bezierCount, String propertyId) {
         super(frameCount, bezierCount, new String[]{propertyId});
      }

      @Override
      public final int getFrameEntries() {
         return 2;
      }

      public final void setFrame(int frame, float time, float value) {
         frame <<= 1;
         super.frames[frame] = time;
         super.frames[frame + 1] = value;
      }

      public final float getCurveValue(float time) {
         float[] frames = super.frames;
         int i = super.frames.length - 2;

         for (int ii = 2; ii <= i; ii += 2) {
            if (frames[ii] > time) {
               i = ii - 2;
               break;
            }
         }

         int curveType;
         switch (curveType = (int)super.PROPERTY_IDS[i >> 1]) {
            case 0:
               float before = frames[i];
               float value;
               float base = value = frames[i + 1];
               float t = time - before;
               t /= frames[i + 2] - before;
               int next = i + 2;
               return base + t * (frames[next + 1] - value);
            case 1:
               return frames[i + 1];
            default:
               return this.getBezierValue(time, i, 1, curveType - 2);
         }
      }

      public final float getRelativeValue(float time, float alpha, int blend, float current, float setup) {
         if (time < super.frames[0]) {
            switch (blend) {
               case MixBlend.SETUP:
                  return setup;
               case MixBlend.FIRST:
                  return current + (setup - current) * alpha;
               default:
                  return current;
            }
         } else {
            float value = this.getCurveValue(time);
            switch (blend) {
               case MixBlend.SETUP:
                  return setup + value * alpha;
               case MixBlend.FIRST:
               case MixBlend.REPLACE:
                  value += setup - current;
            }

            return current + value * alpha;
         }
      }

      public final float getAbsoluteValue(float time, float alpha, int blend, float current, float setup) {
         if (time < super.frames[0]) {
            switch (blend) {
               case MixBlend.SETUP:
                  return setup;
               case MixBlend.FIRST:
                  return current + (setup - current) * alpha;
               default:
                  return current;
            }
         } else {
            float value = this.getCurveValue(time);
            if (blend == Animation.MixBlend.SETUP) {
               return setup + (value - setup) * alpha;
            } else {
               return current + (value - current) * alpha;
            }
         }
      }

      public final float getScaleValue(float time, float alpha, int blend, int direction, float current, float setup) {
         float[] frames = super.frames;
         if (time < frames[0]) {
            switch (blend) {
               case MixBlend.SETUP:
                  return setup;
               case MixBlend.FIRST:
                  return current + (setup - current) * alpha;
               default:
                  return current;
            }
         } else {
            time = this.getCurveValue(time) * setup;
            if (alpha == 1.0F) {
               if (blend == Animation.MixBlend.ADD) {
                  return current + time - setup;
               } else {
                  return time;
               }
            } else {
               if (direction == Animation.MixDirection.OUT) {
                  switch (blend) {
                     case MixBlend.SETUP:
                        return setup + (Math.abs(time) * Math.signum(setup) - setup) * alpha;
                     case MixBlend.FIRST:
                     case MixBlend.REPLACE:
                        return current + (Math.abs(time) * Math.signum(current) - current) * alpha;
                  }
               } else {
                  float s;
                  switch (blend) {
                     case MixBlend.SETUP:
                        s = Math.abs(setup) * Math.signum(time);
                        return s + (time - s) * alpha;
                     case MixBlend.FIRST:
                     case MixBlend.REPLACE:
                        s = Math.abs(current) * Math.signum(time);
                        return s + (time - s) * alpha;
                  }
               }

               return current + (time - setup) * alpha;
            }
         }
      }
   }

   public abstract static class CurveTimeline2 extends Animation.CurveTimeline {
      public CurveTimeline2(int frameCount, int bezierCount, String propertyId1, String propertyId2) {
         super(frameCount, bezierCount, new String[]{propertyId1, propertyId2});
      }

      @Override
      public final int getFrameEntries() {
         return 3;
      }

      public final void setFrame(int frame, float time, float value1, float value2) {
         frame *= 3;
         super.frames[frame] = time;
         super.frames[frame + 1] = value1;
         super.frames[frame + 2] = value2;
      }
   }

   public static class DeformTimeline extends Animation.CurveTimeline {
      public int slotIndex;
      public VertexAttachment attachment;
      public final float[][] vertices;

      public DeformTimeline(int frameCount, int bezierCount, int slotIndex, VertexAttachment attachment) {
         super(frameCount, bezierCount, new String[]{Animation.Property.DEFORM - 1 + "|" + slotIndex + "|" + attachment.getId()});
         this.slotIndex = slotIndex;
         this.attachment = attachment;
         this.vertices = new float[frameCount][];
      }

      @Override
      public final int getFrameCount() {
         return super.frames.length;
      }

      public final void setFrame(int frame, float time, float[] vertices) {
         super.frames[frame] = time;
         this.vertices[frame] = vertices;
      }

      @Override
      public final void setBezier(
         int bezier, int frame, int value, float time1, float value1, float cx1, float cy1, float cx2, float cy2, float time2, float value2
      ) {
         float[] curves = super.PROPERTY_IDS;
         bezier = super.frames.length + bezier * 18;
         if (value == 0) {
            curves[frame] = (float)(bezier + 2);
         }

         float tmpx = (time1 - cx1 * 2.0F + cx2) * 0.03F;
         float tmpy = cy2 * 0.03F - cy1 * 0.06F;
         cx2 = ((cx1 - cx2) * 3.0F - time1 + time2) * 0.006F;
         cy2 = (cy1 - cy2 + 0.33333334F) * 0.018F;
         time2 = tmpx * 2.0F + cx2;
         value2 = tmpy * 2.0F + cy2;
         float dx = (cx1 - time1) * 0.3F + tmpx + cx2 * 0.16666667F;
         float dy = cy1 * 0.3F + tmpy + cy2 * 0.16666667F;
         time1 += dx;
         cx1 = dy;

         for (int n = bezier + 18; bezier < n; bezier += 2) {
            curves[bezier] = time1;
            curves[bezier + 1] = cx1;
            dx += time2;
            dy += value2;
            time2 += cx2;
            value2 += cy2;
            time1 += dx;
            cx1 += dy;
         }
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          int frame;
          Slot slot = skeleton.slots.get(this.slotIndex);
          float[] deform;
          if (!slot.bone.active) {
              return;
          }
          Attachment attachment = slot.attachment;
          if (!(attachment instanceof VertexAttachment) || ((VertexAttachment)attachment).getTimelineAttachment() != this.attachment) {
              return;
          }
          FloatArray deformArray = slot.deform;
          if (deformArray.size == 0) {
              blend = MixBlend.SETUP;
          }
          float[][] vertices = this.vertices;
          direction = vertices[0].length;
          float[] frames = this.frames;
          if (time < frames[0]) {
              switch (blend) {
                  case MixBlend.SETUP: {
                      deformArray.clear();
                      return;
                  }
                  case MixBlend.FIRST: {
                      if (alpha == 1.0f) {
                          deformArray.clear();
                          return;
                      }
                      deform = deformArray.setSize(direction);
                      VertexAttachment vertexAttachment = (VertexAttachment)attachment;
                      if (vertexAttachment.getBones() == null) {
                          float[] setupVertices = vertexAttachment.getVertices();
                          for (int i = 0; i < direction; ++i) {
                              float[] deformArr = deform;
                              int idx = i;
                              deformArr[idx] = deformArr[idx] + (setupVertices[i] - deform[i]) * alpha;
                          }
                          return;
                      }
                      alpha = 1.0f - alpha;
                      for (int i = 0; i < direction; ++i) {
                          deform[i] = deform[i] * alpha;
                      }
                      return;
                  }
              }
              return;
          }
          deform = deformArray.setSize(direction);
          if (time >= frames[frames.length - 1]) {
              float[] lastVertices = vertices[frames.length - 1];
              if (alpha == 1.0f) {
                  if (blend == MixBlend.ADD) {
                      VertexAttachment vertexAttachment = (VertexAttachment)attachment;
                      if (vertexAttachment.getBones() == null) {
                          float[] setupVertices = vertexAttachment.getVertices();
                          for (int i = 0; i < direction; ++i) {
                              float[] deformArr = deform;
                              int idx = i;
                              deformArr[idx] = deformArr[idx] + (lastVertices[i] - setupVertices[i]);
                          }
                          return;
                      }
                      for (int i = 0; i < direction; ++i) {
                          float[] deformArr = deform;
                          int idx = i;
                          deformArr[idx] = deformArr[idx] + lastVertices[i];
                      }
                      return;
                  }
                  SpineUtils.arraycopy(lastVertices, 0, deform, 0, direction);
                  return;
              }
              switch (blend) {
                  case MixBlend.SETUP: {
                      VertexAttachment vertexAttachment = (VertexAttachment)attachment;
                      if (vertexAttachment.getBones() == null) {
                          float[] setupVertices = vertexAttachment.getVertices();
                          for (int i = 0; i < direction; ++i) {
                              float setup = setupVertices[i];
                              deform[i] = setup + (lastVertices[i] - setup) * alpha;
                          }
                          return;
                      }
                      for (int i = 0; i < direction; ++i) {
                          deform[i] = lastVertices[i] * alpha;
                      }
                      return;
                  }
                  case MixBlend.FIRST: 
                  case MixBlend.REPLACE: {
                      for (int i = 0; i < direction; ++i) {
                          float[] deformArr = deform;
                          int idx = i;
                          deformArr[idx] = deformArr[idx] + (lastVertices[i] - deform[i]) * alpha;
                      }
                      return;
                  }
                  case MixBlend.ADD: {
                      VertexAttachment vertexAttachment = (VertexAttachment)attachment;
                      if (vertexAttachment.getBones() == null) {
                          float[] setupVertices = vertexAttachment.getVertices();
                          for (int i = 0; i < direction; ++i) {
                              float[] deformArr = deform;
                              int idx = i;
                              deformArr[idx] = deformArr[idx] + (lastVertices[i] - setupVertices[i]) * alpha;
                          }
                          return;
                      }
                      for (int i = 0; i < direction; ++i) {
                          float[] deformArr = deform;
                          int idx = i;
                          deformArr[idx] = deformArr[idx] + lastVertices[i] * alpha;
                      }
                      return;
                  }
              }
              return;
          }
          int curveFrame = frame = DeformTimeline.search(frames, time);
          float curveTime = time;
          DeformTimeline timeline = this;
          float[] curves = timeline.PROPERTY_IDS;
          int curveIdx = (int)curves[curveFrame];
          float percent;
          percentSwitch:
          switch (curveIdx) {
              case 0: {
                  float x = timeline.frames[curveFrame];
                  percent = (curveTime - x) / (timeline.frames[curveFrame + timeline.getFrameEntries()] - x);
                  break percentSwitch;
              }
              case 1: {percent = 0.0f; break percentSwitch;}
              default: {
                  float bx;
                  float by;
                  curveIdx -= 2;
                  if (curves[curveIdx] > curveTime) {
                      by = timeline.frames[curveFrame];
                      percent = curves[curveIdx + 1] * (curveTime - by) / (curves[curveIdx] - by);
                      break percentSwitch;
                  }
                  int bezierEnd = curveIdx + 18;
                  curveIdx += 2;
                  while (curveIdx < bezierEnd) {
                      if (curves[curveIdx] >= curveTime) {
                          bx = curves[curveIdx - 2];
                          by = curves[curveIdx - 1];
                          percent = by + (curveTime - bx) / (curves[curveIdx] - bx) * (curves[curveIdx + 1] - by);
                          break percentSwitch;
                      }
                      curveIdx += 2;
                  }
                  bx = curves[bezierEnd - 2];
                  by = curves[bezierEnd - 1];
                  percent = by + (1.0f - by) * (curveTime - bx) / (timeline.frames[curveFrame + timeline.getFrameEntries()] - bx);
                  break percentSwitch;
              }
          }
          float[] prevVertices = vertices[frame];
          float[] nextVertices = vertices[frame + 1];
          if (alpha == 1.0f) {
              if (blend == MixBlend.ADD) {
                  if (((VertexAttachment)(attachment = (VertexAttachment)attachment)).getBones() == null) {
                      float[] setupVertices = ((VertexAttachment)attachment).getVertices();
                      for (int i = 0; i < direction; ++i) {
                          float prev = prevVertices[i];
                          float[] deformArr = deform;
                          int idx = i;
                          deformArr[idx] = deformArr[idx] + (prev + (nextVertices[i] - prev) * percent - setupVertices[i]);
                      }
                      return;
                  }
                  for (blend = 0; blend < direction; ++blend) {
                      float prev = prevVertices[blend];
                      float[] deformArr = deform;
                      int idx = blend;
                      deformArr[idx] = deformArr[idx] + (prev + (nextVertices[blend] - prev) * percent);
                  }
                  return;
              }
              for (int i = 0; i < direction; ++i) {
                  float prev = prevVertices[i];
                  deform[i] = prev + (nextVertices[i] - prev) * percent;
              }
              return;
          }
          switch (blend) {
              case MixBlend.SETUP: {
                  attachment = (VertexAttachment)attachment;
                  if (((VertexAttachment)attachment).getBones() == null) {
                      float[] setupVertices = ((VertexAttachment)attachment).getVertices();
                      for (int i = 0; i < direction; ++i) {
                          float prev = prevVertices[i];
                          float setup = setupVertices[i];
                          deform[i] = setup + (prev + (nextVertices[i] - prev) * percent - setup) * alpha;
                      }
                      return;
                  }
                  for (blend = 0; blend < direction; ++blend) {
                      float prev = prevVertices[blend];
                      deform[blend] = (prev + (nextVertices[blend] - prev) * percent) * alpha;
                  }
                  return;
              }
              case MixBlend.FIRST: 
              case MixBlend.REPLACE: {
                  for (int i = 0; i < direction; ++i) {
                      float prev = prevVertices[i];
                      float[] deformArr = deform;
                      int idx = i;
                      deformArr[idx] = deformArr[idx] + (prev + (nextVertices[i] - prev) * percent - deform[i]) * alpha;
                  }
                  return;
              }
              case MixBlend.ADD: {
                  attachment = (VertexAttachment)attachment;
                  if (((VertexAttachment)attachment).getBones() == null) {
                      float[] setupVertices = ((VertexAttachment)attachment).getVertices();
                      for (int i = 0; i < direction; ++i) {
                          float prev = prevVertices[i];
                          float[] deformArr = deform;
                          int idx = i;
                          deformArr[idx] = deformArr[idx] + (prev + (nextVertices[i] - prev) * percent - setupVertices[i]) * alpha;
                      }
                      return;
                  }
                  for (blend = 0; blend < direction; ++blend) {
                      float prev = prevVertices[blend];
                      float[] deformArr = deform;
                      int idx = blend;
                      deformArr[idx] = deformArr[idx] + (prev + (nextVertices[blend] - prev) * percent) * alpha;
                  }
                  return;
              }
          }
      }
   }

   public static class DrawOrderTimeline extends Animation.Timeline {
      public static final String[] PROPERTY_IDS;
      public final int[][] drawOrders;

      public DrawOrderTimeline(int frameCount) {
         super(frameCount, PROPERTY_IDS);
         this.drawOrders = new int[frameCount][];
      }

      @Override
      public final int getFrameCount() {
         return super.frames.length;
      }

      public final void setFrame(int frame, float time, @Null int[] drawOrder) {
         super.frames[frame] = time;
         this.drawOrders[frame] = drawOrder;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         if (direction == Animation.MixDirection.OUT) {
            if (blend == Animation.MixBlend.SETUP) {
               SpineUtils.arraycopy(skeleton.slots.items, 0, skeleton.drawOrder.items, 0, skeleton.slots.size);
            }
         } else if (time < super.frames[0]) {
            if (blend == Animation.MixBlend.SETUP || blend == Animation.MixBlend.FIRST) {
               SpineUtils.arraycopy(skeleton.slots.items, 0, skeleton.drawOrder.items, 0, skeleton.slots.size);
            }
         } else {
            int[] drawOrderToSetupIndex;
            if ((drawOrderToSetupIndex = this.drawOrders[search(super.frames, time)]) == null) {
               SpineUtils.arraycopy(
                  skeleton.slots.items, 0, skeleton.drawOrder.items, 0, skeleton.slots.size
               );
            } else {
               Object[] slots = skeleton.slots.items;
               Object[] drawOrder = skeleton.drawOrder.items;
               int i = 0;

               for (int n = drawOrderToSetupIndex.length; i < n; i++) {
                  drawOrder[i] = slots[drawOrderToSetupIndex[i]];
               }
            }
         }
      }

      static {
         String[] ids = new String[1];
         ids[0] = Integer.toString(Animation.Property.DRAW_ORDER - 1);
         PROPERTY_IDS = ids;
      }
   }

   public static class EventTimeline extends Animation.Timeline {
      public static final String[] PROPERTY_IDS;
      public final Event[] events;

      public EventTimeline(int frameCount) {
         super(frameCount, PROPERTY_IDS);
         this.events = new Event[frameCount];
      }

      @Override
      public final int getFrameCount() {
         return super.frames.length;
      }

      public final void setFrame(int frame, Event event) {
         super.frames[frame] = event.time;
         this.events[frame] = event;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         if (events != null) {
            float[] frames = super.frames;
            int frameCount = super.frames.length;
            if (lastTime > time) {
               this.apply(skeleton, lastTime, 2.1474836E9F, events, alpha, blend, direction);
               lastTime = -1.0F;
            } else {
               if (lastTime >= frames[frameCount - 1]) {
                  return;
               }
            }

            if (!(time < frames[0])) {
               int i;
               if (lastTime < frames[0]) {
                  i = 0;
               } else {
                  i = search(frames, lastTime) + 1;

                  for (float frameTime = frames[i]; i > 0; i--) {
                     if (frames[i - 1] != frameTime) {
                        break;
                     }
                  }
               }

               while (i < frameCount && time >= frames[i]) {
                  events.add(this.events[i]);
                  i++;
               }
            }
         }
      }

      static {
         String[] ids = new String[1];
         ids[0] = Integer.toString(Animation.Property.EVENT - 1);
         PROPERTY_IDS = ids;
      }
   }

   public static class IkConstraintTimeline extends Animation.CurveTimeline {
      public int constraintIndex;

      public IkConstraintTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, new String[]{Animation.Property.IK_CONSTRAINT - 1 + "|" + constraintIndex});
         this.constraintIndex = constraintIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 6;
      }

      public final void setFrame(int frame, float time, float mix, float softness, int bendDirection, boolean compress, boolean stretch) {
         frame *= 6;
         super.frames[frame] = time;
         super.frames[frame + 1] = mix;
         super.frames[frame + 2] = softness;
         super.frames[frame + 3] = (float)bendDirection;
         super.frames[frame + 4] = compress ? 1.0F : 0.0F;
         super.frames[frame + 5] = stretch ? 1.0F : 0.0F;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float softness;
          float mix;
          int unused;
          int i;
          float[] frames;
          IkConstraint constraint = skeleton.ikConstraints.get(this.constraintIndex);
          if (!constraint.active) {
              return;
          }
          frames = this.frames;
          if (time < frames[0]) {
              switch (blend) {
                  case MixBlend.SETUP: {
                      constraint.mix = constraint.data.mix;
                      constraint.softness = constraint.data.softness;
                      constraint.bendDirection = constraint.data.bendDirection;
                      constraint.compress = constraint.data.compress;
                      constraint.stretch = constraint.data.stretch;
                      return;
                  }
                  case MixBlend.FIRST: {
                      constraint.mix += (constraint.data.mix - constraint.mix) * alpha;
                      constraint.softness += (constraint.data.softness - constraint.softness) * alpha;
                      constraint.bendDirection = constraint.data.bendDirection;
                      constraint.compress = constraint.data.compress;
                      constraint.stretch = constraint.data.stretch;
                  }
              }
              return;
          }
          i = IkConstraintTimeline.search(frames, time, 6);
          int curveType = (int)this.PROPERTY_IDS[i / 6];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  mix = frames[i + 1];
                  softness = frames[i + 2];
                  time = (time - before) / (frames[i + 6] - before);
                  mix += (frames[i + 6 + 1] - mix) * time;
                  softness += (frames[i + 6 + 2] - softness) * time;
                  break;
              }
              case 1: {
                  mix = frames[i + 1];
                  softness = frames[i + 2];
                  break;
              }
              default: {
                  mix = this.getBezierValue(time, i, 1, curveType - 2);
                  softness = this.getBezierValue(time, i, 2, curveType + 18 - 2);
              }
          }
          boolean stretch;
          merged: {
          if (blend == MixBlend.SETUP) {
              constraint.mix = constraint.data.mix + (mix - constraint.data.mix) * alpha;
              constraint.softness = constraint.data.softness + (softness - constraint.data.softness) * alpha;
              if (direction == MixDirection.OUT) {
                  constraint.bendDirection = constraint.data.bendDirection;
                  constraint.compress = constraint.data.compress;
                  stretch = constraint.data.stretch;
                  break merged;
              }
              constraint.bendDirection = (int)frames[i + 3];
              constraint.compress = frames[i + 4] != 0.0f;
              constraint.stretch = frames[i + 5] != 0.0f;
              return;
          }
          constraint.mix += (mix - constraint.mix) * alpha;
          constraint.softness += (softness - constraint.softness) * alpha;
          if (direction != MixDirection.IN) {
              return;
          }
          constraint.bendDirection = (int)frames[i + 3];
          constraint.compress = frames[i + 4] != 0.0f;
          stretch = frames[i + 5] != 0.0f;
          }
          constraint.stretch = stretch;
      }
   }

   public static class InheritTimeline extends Animation.Timeline {
      public int PROPERTY_IDS;

      public InheritTimeline(int frameCount, int boneIndex) {
         super(frameCount, new String[]{Animation.Property.INHERIT - 1 + "|" + boneIndex});
         this.PROPERTY_IDS = boneIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 2;
      }

      public final void setFrame(int frame, float time, int inherit) {
         frame <<= 1;
         super.frames[frame] = time;
         int valueIndex = frame + 1;
         super.frames[valueIndex] = (float)(inherit - 1);
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.PROPERTY_IDS)).active) {
            if (direction == Animation.MixDirection.OUT) {
               if (blend == Animation.MixBlend.SETUP) {
                  bone.inherit = bone.data.inherit;
               }
            } else {
               float[] frames = super.frames;
               if (!(time < frames[0])) {
                  int[] inherits = BoneData.Inherit.VALUES;
                  int i = search(frames, time, 2);
                  bone.inherit = inherits[(int)frames[i + 1]];
               } else {
                  if (blend == Animation.MixBlend.SETUP || blend == Animation.MixBlend.FIRST) {
                     bone.inherit = bone.data.inherit;
                  }
               }
            }
         }
      }
   }

   public static final class MixBlend {
      public static final int SETUP = 1;
      public static final int FIRST = 2;
      public static final int REPLACE = 3;
      public static final int ADD = 4;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[4];
         values[0] = SETUP;
         values[1] = FIRST;
         values[2] = REPLACE;
         values[3] = ADD;
         VALUES = values;
      }
   }

   public static final class MixDirection {
      public static final int IN;
      public static final int OUT = 2;

      static {
         IN = 1;
         int[] values = new int[2];
         values[0] = IN;
         values[1] = OUT;
      }
   }

   public static class PathConstraintMixTimeline extends Animation.CurveTimeline {
      public int constraintIndex;

      public PathConstraintMixTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, new String[]{Animation.Property.PATH_CONSTRAINT_MIX - 1 + "|" + constraintIndex});
         this.constraintIndex = constraintIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 4;
      }

      public final void setFrame(int frame, float time, float mixRotate, float mixX, float mixY) {
         frame <<= 2;
         super.frames[frame] = time;
         super.frames[frame + 1] = mixRotate;
         super.frames[frame + 2] = mixX;
         super.frames[frame + 3] = mixY;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float y;
          float x;
          float rotate;
          PathConstraint constraint = skeleton.pathConstraints.get(this.constraintIndex);
          if (!constraint.active) {
              return;
          }
          float[] frames = this.frames;
          if (time < frames[0]) {
              PathConstraintData data = constraint.data;
              switch (blend) {
                  case MixBlend.SETUP: {
                      constraint.mixRotate = data.mixRotate;
                      constraint.mixX = data.mixX;
                      constraint.mixY = data.mixY;
                      return;
                  }
                  case MixBlend.FIRST: {
                      constraint.mixRotate += (data.mixRotate - constraint.mixRotate) * alpha;
                      constraint.mixX += (data.mixX - constraint.mixX) * alpha;
                      constraint.mixY += (data.mixY - constraint.mixY) * alpha;
                  }
              }
              return;
          }
          int i = PathConstraintMixTimeline.search(frames, time, 4);
          int curveType = (int)this.PROPERTY_IDS[i >> 2];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  rotate = frames[i + 1];
                  x = frames[i + 2];
                  y = frames[i + 3];
                  time = (time - before) / (frames[i + 4] - before);
                  rotate += (frames[i + 4 + 1] - rotate) * time;
                  x += (frames[(i + 4) + 2] - x) * time;
                  y += (frames[i + 4 + 3] - y) * time;
                  break;
              }
              case 1: {
                  rotate = frames[i + 1];
                  x = frames[i + 2];
                  y = frames[i + 3];
                  break;
              }
              default: {
                  rotate = this.getBezierValue(time, i, 1, curveType - 2);
                  x = this.getBezierValue(time, i, 2, curveType + 18 - 2);
                  y = this.getBezierValue(time, i, 3, (curveType + 36) - 2);
              }
          }
          if (blend == MixBlend.SETUP) {
              PathConstraintData data = constraint.data;
              constraint.mixRotate = data.mixRotate + (rotate - data.mixRotate) * alpha;
              constraint.mixX = data.mixX + (x - data.mixX) * alpha;
              constraint.mixY = data.mixY + (y - data.mixY) * alpha;
              return;
          }
          constraint.mixRotate += (rotate - constraint.mixRotate) * alpha;
          constraint.mixX += (x - constraint.mixX) * alpha;
          constraint.mixY += (y - constraint.mixY) * alpha;
      }
   }

   public static class PathConstraintPositionTimeline extends Animation.CurveTimeline1 {
      public int constraintIndex;

      public PathConstraintPositionTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, Animation.Property.PATH_CONSTRAINT_POSITION - 1 + "|" + constraintIndex);
         this.constraintIndex = constraintIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         PathConstraint constraint;
         if ((constraint = skeleton.pathConstraints.get(this.constraintIndex)).active) {
            constraint.position = this.getAbsoluteValue(time, alpha, blend, constraint.position, constraint.data.position);
         }
      }
   }

   public static class PathConstraintSpacingTimeline extends Animation.CurveTimeline1 {
      public int constraintIndex;

      public PathConstraintSpacingTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, Animation.Property.PATH_CONSTRAINT_SPACING - 1 + "|" + constraintIndex);
         this.constraintIndex = constraintIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         PathConstraint constraint;
         if ((constraint = skeleton.pathConstraints.get(this.constraintIndex)).active) {
            constraint.spacing = this.getAbsoluteValue(time, alpha, blend, constraint.spacing, constraint.data.spacing);
         }
      }
   }

   public static class PhysicsConstraintDampingTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintDampingTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_DAMPING);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return constraint.data.damping;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return constraint.damping;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.damping = value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.dampingGlobal;
      }
   }

   public static class PhysicsConstraintGravityTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintGravityTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_GRAVITY);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return constraint.data.gravity;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return constraint.gravity;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.gravity = value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.gravityGlobal;
      }
   }

   public static class PhysicsConstraintInertiaTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintInertiaTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_INERTIA);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return constraint.data.inertia;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return constraint.inertia;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.inertia = value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.inertiaGlobal;
      }
   }

   public static class PhysicsConstraintMassTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintMassTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_MASS);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return 1.0F / constraint.data.massInverse;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return 1.0F / constraint.massInverse;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.massInverse = 1.0F / value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.massGlobal;
      }
   }

   public static class PhysicsConstraintMixTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintMixTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_MIX);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return constraint.data.mix;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return constraint.mix;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.mix = value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.mixGlobal;
      }
   }

   public static class PhysicsConstraintResetTimeline extends Animation.Timeline {
      public static final String[] PROPERTY_IDS;
      public int constraintIndex;

      public PhysicsConstraintResetTimeline(int frameCount, int constraintIndex) {
         super(frameCount, PROPERTY_IDS);
         this.constraintIndex = constraintIndex;
      }

      @Override
      public final int getFrameCount() {
         return super.frames.length;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         PhysicsConstraint constraint = null;
         if (this.constraintIndex == -1 || (constraint = skeleton.physicsConstraints.get(this.constraintIndex)).active) {
            float[] frames = super.frames;
            if (lastTime > time) {
               this.apply(skeleton, lastTime, 2.1474836E9F, null, alpha, blend, direction);
               lastTime = -1.0F;
            } else if (lastTime >= frames[frames.length - 1]) {
               return;
            }

            if (!(time < frames[0])) {
               if (lastTime < frames[0] || time >= frames[search(frames, lastTime) + 1]) {
                  if (constraint != null) {
                     constraint.reset();
                     return;
                  }

                  Object[] constraints = skeleton.physicsConstraints.items;
                  int i = 0;

                  for (int n = skeleton.physicsConstraints.size; i < n; i++) {
                     PhysicsConstraint c;
                     if ((c = (PhysicsConstraint)constraints[i]).active) {
                        c.reset();
                     }
                  }
               }
            }
         }
      }

      static {
         String[] ids = new String[1];
         ids[0] = Integer.toString(Animation.Property.PHYSICS_CONSTRAINT_RESET - 1);
         PROPERTY_IDS = ids;
      }
   }

   public static class PhysicsConstraintStrengthTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintStrengthTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_STRENGTH);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return constraint.data.strength;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return constraint.strength;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.strength = value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.strengthGlobal;
      }
   }

   public abstract static class PhysicsConstraintTimeline extends Animation.CurveTimeline1 {
      public int constraintIndex;

      public PhysicsConstraintTimeline(int frameCount, int bezierCount, int constraintIndex, int property) {
         super(frameCount, bezierCount, property - 1 + "|" + constraintIndex);
         this.constraintIndex = constraintIndex;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          if (this.constraintIndex == -1) {
              lastTime = time >= this.frames[0] ? this.getCurveValue(time) : 0.0f;
              Object[] constraints = skeleton.physicsConstraints.items;
              int n = skeleton.physicsConstraints.size;
              for (direction = 0; direction < n; ++direction) {
                  float result;
                  PhysicsConstraint constraint = (PhysicsConstraint)constraints[direction];
                  if (!constraint.active || !this.isGlobal(constraint.data)) continue;
                  PhysicsConstraint target = constraint;
                  float valueArg = lastTime;
                  float current = this.getCurrentValue(constraint);
                  float setup = this.getSetupValue(constraint);
                  int blendArg = blend;
                  float alphaArg = alpha;
                  float timeArg = time;
                  PhysicsConstraintTimeline timeline = this;
                  if (timeArg < timeline.frames[0]) {
                      switch (blendArg) {
                          case MixBlend.SETUP: {
                              result = setup;
                              break;
                          }
                          case MixBlend.FIRST: {
                              result = current + (setup - current) * alphaArg;
                              break;
                          }
                          default: {
                              result = current;
                              break;
                          }
                      }
                  } else {
                      result = blendArg == MixBlend.SETUP ? setup + (valueArg - setup) * alphaArg : current + (valueArg - current) * alphaArg;
                  }
                  this.setValue(target, result);
              }
              return;
          }
          PhysicsConstraint constraint = skeleton.physicsConstraints.get(this.constraintIndex);
          if (constraint.active) {
              this.setValue(constraint, this.getAbsoluteValue(time, alpha, blend, this.getCurrentValue(constraint), this.getSetupValue(constraint)));
          }
      }

      public abstract float getSetupValue(PhysicsConstraint constraint);

      public abstract float getCurrentValue(PhysicsConstraint constraint);

      public abstract void setValue(PhysicsConstraint constraint, float value);

      public abstract boolean isGlobal(PhysicsConstraintData data);
   }

   public static class PhysicsConstraintWindTimeline extends Animation.PhysicsConstraintTimeline {
      public PhysicsConstraintWindTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, constraintIndex, Animation.Property.PHYSICS_CONSTRAINT_WIND);
      }

      @Override
      public final float getSetupValue(PhysicsConstraint constraint) {
         return constraint.data.wind;
      }

      @Override
      public final float getCurrentValue(PhysicsConstraint constraint) {
         return constraint.wind;
      }

      @Override
      public final void setValue(PhysicsConstraint constraint, float value) {
         constraint.wind = value;
      }

      @Override
      public final boolean isGlobal(PhysicsConstraintData data) {
         return data.windGlobal;
      }
   }

   public static final class Property {
      public static final int ROTATE;
      public static final int X = 2;
      public static final int Y = 3;
      public static final int SCALE_X = 4;
      public static final int SCALE_Y = 5;
      public static final int SHEAR_X;
      public static final int SHEAR_Y = 7;
      public static final int INHERIT;
      public static final int RGB;
      public static final int ALPHA;
      public static final int RGB2;
      public static final int ATTACHMENT;
      public static final int DEFORM = 13;
      public static final int EVENT;
      public static final int DRAW_ORDER;
      public static final int IK_CONSTRAINT;
      public static final int TRANSFORM_CONSTRAINT;
      public static final int PATH_CONSTRAINT_POSITION;
      public static final int PATH_CONSTRAINT_SPACING = 19;
      public static final int PATH_CONSTRAINT_MIX;
      public static final int PHYSICS_CONSTRAINT_INERTIA = 21;
      public static final int PHYSICS_CONSTRAINT_STRENGTH = 22;
      public static final int PHYSICS_CONSTRAINT_DAMPING = 23;
      public static final int PHYSICS_CONSTRAINT_MASS = 24;
      public static final int PHYSICS_CONSTRAINT_WIND = 25;
      public static final int PHYSICS_CONSTRAINT_GRAVITY = 26;
      public static final int PHYSICS_CONSTRAINT_MIX = 27;
      public static final int PHYSICS_CONSTRAINT_RESET;
      public static final int SEQUENCE = 29;

      static {
         ROTATE = 1;
         SHEAR_X = 6;
         INHERIT = 8;
         RGB = 9;
         ALPHA = 10;
         RGB2 = 11;
         ATTACHMENT = 12;
         EVENT = 14;
         DRAW_ORDER = 15;
         IK_CONSTRAINT = 16;
         TRANSFORM_CONSTRAINT = 17;
         PATH_CONSTRAINT_POSITION = 18;
         PATH_CONSTRAINT_MIX = 20;
         PHYSICS_CONSTRAINT_RESET = 28;
         int[] values = new int[29];
         values[0] = ROTATE;
         values[1] = X;
         values[2] = Y;
         values[3] = SCALE_X;
         values[4] = SCALE_Y;
         values[5] = SHEAR_X;
         values[6] = SHEAR_Y;
         values[7] = INHERIT;
         values[8] = RGB;
         values[9] = ALPHA;
         values[10] = RGB2;
         values[11] = ATTACHMENT;
         values[12] = DEFORM;
         values[13] = EVENT;
         values[14] = DRAW_ORDER;
         values[15] = IK_CONSTRAINT;
         values[16] = TRANSFORM_CONSTRAINT;
         values[17] = PATH_CONSTRAINT_POSITION;
         values[18] = PATH_CONSTRAINT_SPACING;
         values[19] = PATH_CONSTRAINT_MIX;
         values[20] = PHYSICS_CONSTRAINT_INERTIA;
         values[21] = PHYSICS_CONSTRAINT_STRENGTH;
         values[22] = PHYSICS_CONSTRAINT_DAMPING;
         values[23] = PHYSICS_CONSTRAINT_MASS;
         values[24] = PHYSICS_CONSTRAINT_WIND;
         values[25] = PHYSICS_CONSTRAINT_GRAVITY;
         values[26] = PHYSICS_CONSTRAINT_MIX;
         values[27] = PHYSICS_CONSTRAINT_RESET;
         values[28] = SEQUENCE;
      }
   }

   public static class RGB2Timeline extends Animation.CurveTimeline {
      public int slotIndex;

      public RGB2Timeline(int frameCount, int bezierCount, int slotIndex) {
         super(frameCount, bezierCount, new String[]{Animation.Property.RGB - 1 + "|" + slotIndex, Animation.Property.RGB2 - 1 + "|" + slotIndex});
         this.slotIndex = slotIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 7;
      }

      public final void setFrame(int frame, float time, float r, float g, float b, float r2, float g2, float b2) {
         frame *= 7;
         super.frames[frame] = time;
         super.frames[frame + 1] = r;
         super.frames[frame + 2] = g;
         super.frames[frame + 3] = b;
         super.frames[frame + 4] = r2;
         super.frames[frame + 5] = g2;
         super.frames[frame + 6] = b2;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float b2;
          float g2;
          float r2;
          float b;
          float g;
          float r;
          Slot slot = skeleton.slots.get(this.slotIndex);
          if (!slot.bone.active) {
              return;
          }
          float[] frames = this.frames;
          Color light = slot.color;
          Color dark = slot.darkColor;
          if (time < frames[0]) {
              Color setupLight = slot.data.color;
              Color setupDark = slot.data.darkColor;
              switch (blend) {
                  case MixBlend.SETUP: {
                      light.r = setupLight.r;
                      light.g = setupLight.g;
                      light.b = setupLight.b;
                      dark.r = setupDark.r;
                      dark.g = setupDark.g;
                      dark.b = setupDark.b;
                      return;
                  }
                  case MixBlend.FIRST: {
                      light.r += (setupLight.r - light.r) * alpha;
                      light.g += (setupLight.g - light.g) * alpha;
                      light.b += (setupLight.b - light.b) * alpha;
                      dark.r += (setupDark.r - dark.r) * alpha;
                      dark.g += (setupDark.g - dark.g) * alpha;
                      dark.b += (setupDark.b - dark.b) * alpha;
                  }
              }
              return;
          }
          int i = RGB2Timeline.search(frames, time, 7);
          int curveType = (int)this.PROPERTY_IDS[i / 7];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  r2 = frames[i + 4];
                  g2 = frames[i + 5];
                  b2 = frames[i + 6];
                  time = (time - before) / (frames[i + 7] - before);
                  r += (frames[i + 7 + 1] - r) * time;
                  g += (frames[(i + 7) + 2] - g) * time;
                  b += (frames[i + 7 + 3] - b) * time;
                  r2 += (frames[i + 7 + 4] - r2) * time;
                  g2 += (frames[i + 7 + 5] - g2) * time;
                  b2 += (frames[i + 7 + 6] - b2) * time;
                  break;
              }
              case 1: {
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  r2 = frames[i + 4];
                  g2 = frames[i + 5];
                  b2 = frames[i + 6];
                  break;
              }
              default: {
                  r = this.getBezierValue(time, i, 1, curveType - 2);
                  g = this.getBezierValue(time, i, 2, curveType + 18 - 2);
                  b = this.getBezierValue(time, i, 3, (curveType + 36) - 2);
                  r2 = this.getBezierValue(time, i, 4, curveType + 54 - 2);
                  g2 = this.getBezierValue(time, i, 5, curveType + 72 - 2);
                  b2 = this.getBezierValue(time, i, 6, curveType + 90 - 2);
              }
          }
          if (alpha == 1.0f) {
              light.r = r;
              light.g = g;
              light.b = b;
              dark.r = r2;
              dark.g = g2;
              dark.b = b2;
              return;
          }
          if (blend == MixBlend.SETUP) {
              Color setupLight = slot.data.color;
              Color setupDark = slot.data.darkColor;
              light.r = setupLight.r;
              light.g = setupLight.g;
              light.b = setupLight.b;
              dark.r = setupDark.r;
              dark.g = setupDark.g;
              dark.b = setupDark.b;
          }
          light.r += (r - light.r) * alpha;
          light.g += (g - light.g) * alpha;
          light.b += (b - light.b) * alpha;
          dark.r += (r2 - dark.r) * alpha;
          dark.g += (g2 - dark.g) * alpha;
          dark.b += (b2 - dark.b) * alpha;
      }
   }

   public static class RGBA2Timeline extends Animation.CurveTimeline {
      public int slotIndex;

      public RGBA2Timeline(int frameCount, int bezierCount, int slotIndex) {
         super(frameCount, bezierCount, new String[]{
            Animation.Property.RGB - 1 + "|" + slotIndex,
            Animation.Property.ALPHA - 1 + "|" + slotIndex,
            Animation.Property.RGB2 - 1 + "|" + slotIndex
         });
         this.slotIndex = slotIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 8;
      }

      public final void setFrame(int frame, float time, float r, float g, float b, float a, float r2, float g2, float b2) {
         frame <<= 3;
         super.frames[frame] = time;
         super.frames[frame + 1] = r;
         super.frames[frame + 2] = g;
         super.frames[frame + 3] = b;
         super.frames[frame + 4] = a;
         super.frames[frame + 5] = r2;
         super.frames[frame + 6] = g2;
         super.frames[frame + 7] = b2;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float b2;
          float g2;
          float r2;
          float a;
          float b;
          float g;
          float r;
          Slot slot = skeleton.slots.get(this.slotIndex);
          if (!slot.bone.active) {
              return;
          }
          float[] frames = this.frames;
          Color light = slot.color;
          Color dark = slot.darkColor;
          if (time < frames[0]) {
              Color setupLight = slot.data.color;
              Color setupDark = slot.data.darkColor;
              switch (blend) {
                  case MixBlend.SETUP: {
                      light.set(setupLight);
                      dark.r = setupDark.r;
                      dark.g = setupDark.g;
                      dark.b = setupDark.b;
                      return;
                  }
                  case MixBlend.FIRST: {
                      light.add((setupLight.r - light.r) * alpha, (setupLight.g - light.g) * alpha, (setupLight.b - light.b) * alpha, (setupLight.a - light.a) * alpha);
                      dark.r += (setupDark.r - dark.r) * alpha;
                      dark.g += (setupDark.g - dark.g) * alpha;
                      dark.b += (setupDark.b - dark.b) * alpha;
                  }
              }
              return;
          }
          int i = RGBA2Timeline.search(frames, time, 8);
          int curveType = (int)this.PROPERTY_IDS[i >> 3];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  a = frames[i + 4];
                  r2 = frames[i + 5];
                  g2 = frames[i + 6];
                  b2 = frames[i + 7];
                  time = (time - before) / (frames[i + 8] - before);
                  r += (frames[i + 8 + 1] - r) * time;
                  g += (frames[i + 8 + 2] - g) * time;
                  b += (frames[i + 8 + 3] - b) * time;
                  a += (frames[i + 8 + 4] - a) * time;
                  r2 += (frames[i + 8 + 5] - r2) * time;
                  g2 += (frames[(i + 8) + 6] - g2) * time;
                  b2 += (frames[i + 8 + 7] - b2) * time;
                  break;
              }
              case 1: {
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  a = frames[i + 4];
                  r2 = frames[i + 5];
                  g2 = frames[i + 6];
                  b2 = frames[i + 7];
                  break;
              }
              default: {
                  r = this.getBezierValue(time, i, 1, curveType - 2);
                  g = this.getBezierValue(time, i, 2, (curveType + 18) - 2);
                  b = this.getBezierValue(time, i, 3, curveType + 36 - 2);
                  a = this.getBezierValue(time, i, 4, curveType + 54 - 2);
                  r2 = this.getBezierValue(time, i, 5, (curveType + 72) - 2);
                  g2 = this.getBezierValue(time, i, 6, curveType + 90 - 2);
                  b2 = this.getBezierValue(time, i, 7, curveType + 108 - 2);
              }
          }
          if (alpha == 1.0f) {
              light.set(r, g, b, a);
              dark.r = r2;
              dark.g = g2;
              dark.b = b2;
              return;
          }
          if (blend == MixBlend.SETUP) {
              light.set(slot.data.color);
              Color setupDark = slot.data.darkColor;
              dark.r = setupDark.r;
              dark.g = setupDark.g;
              dark.b = setupDark.b;
          }
          light.add((r - light.r) * alpha, (g - light.g) * alpha, (b - light.b) * alpha, (a - light.a) * alpha);
          dark.r += (r2 - dark.r) * alpha;
          dark.g += (g2 - dark.g) * alpha;
          dark.b += (b2 - dark.b) * alpha;
      }
   }

   public static class RGBATimeline extends Animation.CurveTimeline {
      public int slotIndex;

      public RGBATimeline(int frameCount, int bezierCount, int slotIndex) {
         super(frameCount, bezierCount, new String[]{
            Animation.Property.RGB - 1 + "|" + slotIndex,
            Animation.Property.ALPHA - 1 + "|" + slotIndex
         });
         this.slotIndex = slotIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 5;
      }

      public final void setFrame(int frame, float time, float r, float g, float b, float a) {
         frame *= 5;
         super.frames[frame] = time;
         super.frames[frame + 1] = r;
         super.frames[frame + 2] = g;
         super.frames[frame + 3] = b;
         super.frames[frame + 4] = a;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float a;
          float b;
          float g;
          float r;
          Slot slot = skeleton.slots.get(this.slotIndex);
          if (!slot.bone.active) {
              return;
          }
          float[] frames = this.frames;
          Color color = slot.color;
          if (time < frames[0]) {
              Color setup = slot.data.color;
              switch (blend) {
                  case MixBlend.SETUP: {
                      color.set(setup);
                      return;
                  }
                  case MixBlend.FIRST: {
                      color.add((setup.r - color.r) * alpha, (setup.g - color.g) * alpha, (setup.b - color.b) * alpha, (setup.a - color.a) * alpha);
                  }
              }
              return;
          }
          int i = RGBATimeline.search(frames, time, 5);
          int curveType = (int)this.PROPERTY_IDS[i / 5];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  a = frames[i + 4];
                  time = (time - before) / (frames[i + 5] - before);
                  r += (frames[(i + 5) + 1] - r) * time;
                  g += (frames[i + 5 + 2] - g) * time;
                  b += (frames[(i + 5) + 3] - b) * time;
                  a += (frames[i + 5 + 4] - a) * time;
                  break;
              }
              case 1: {
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  a = frames[i + 4];
                  break;
              }
              default: {
                  r = this.getBezierValue(time, i, 1, curveType - 2);
                  g = this.getBezierValue(time, i, 2, (curveType + 18) - 2);
                  b = this.getBezierValue(time, i, 3, curveType + 36 - 2);
                  a = this.getBezierValue(time, i, 4, curveType + 54 - 2);
              }
          }
          if (alpha == 1.0f) {
              color.set(r, g, b, a);
              return;
          }
          if (blend == MixBlend.SETUP) {
              color.set(slot.data.color);
          }
          color.add((r - color.r) * alpha, (g - color.g) * alpha, (b - color.b) * alpha, (a - color.a) * alpha);
      }
   }

   public static class RGBTimeline extends Animation.CurveTimeline {
      public int slotIndex;

      public RGBTimeline(int frameCount, int bezierCount, int slotIndex) {
         super(frameCount, bezierCount, new String[]{Animation.Property.RGB - 1 + "|" + slotIndex});
         this.slotIndex = slotIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 4;
      }

      public final void setFrame(int frame, float time, float r, float g, float b) {
         frame <<= 2;
         super.frames[frame] = time;
         super.frames[frame + 1] = r;
         super.frames[frame + 2] = g;
         super.frames[frame + 3] = b;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float b;
          float g;
          float r;
          Slot slot = skeleton.slots.get(this.slotIndex);
          if (!slot.bone.active) {
              return;
          }
          float[] frames = this.frames;
          Color color = slot.color;
          if (time < frames[0]) {
              Color setup = slot.data.color;
              switch (blend) {
                  case MixBlend.SETUP: {
                      color.r = setup.r;
                      color.g = setup.g;
                      color.b = setup.b;
                      return;
                  }
                  case MixBlend.FIRST: {
                      color.r += (setup.r - color.r) * alpha;
                      color.g += (setup.g - color.g) * alpha;
                      color.b += (setup.b - color.b) * alpha;
                  }
              }
              return;
          }
          int i = RGBTimeline.search(frames, time, 4);
          int curveType = (int)this.PROPERTY_IDS[i >> 2];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  time = (time - before) / (frames[i + 4] - before);
                  r += (frames[(i + 4) + 1] - r) * time;
                  g += (frames[(i + 4) + 2] - g) * time;
                  b += (frames[(i + 4) + 3] - b) * time;
                  break;
              }
              case 1: {
                  r = frames[i + 1];
                  g = frames[i + 2];
                  b = frames[i + 3];
                  break;
              }
              default: {
                  r = this.getBezierValue(time, i, 1, curveType - 2);
                  g = this.getBezierValue(time, i, 2, (curveType + 18) - 2);
                  b = this.getBezierValue(time, i, 3, (curveType + 36) - 2);
              }
          }
          if (alpha == 1.0f) {
              color.r = r;
              color.g = g;
              color.b = b;
              return;
          }
          if (blend == MixBlend.SETUP) {
              Color setup = slot.data.color;
              color.r = setup.r;
              color.g = setup.g;
              color.b = setup.b;
          }
          color.r += (r - color.r) * alpha;
          color.g += (g - color.g) * alpha;
          color.b += (b - color.b) * alpha;
      }
   }

   public static class RotateTimeline extends Animation.CurveTimeline1 {
      public final int boneIndex;

      public RotateTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.ROTATE - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.rotation = this.getRelativeValue(time, alpha, blend, bone.rotation, bone.data.rotation);
         }
      }
   }

   public static class ScaleTimeline extends Animation.CurveTimeline2 {
      public int boneIndex;

      public ScaleTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(
            frameCount, bezierCount, Animation.Property.SCALE_X - 1 + "|" + boneIndex, Animation.Property.SCALE_Y - 1 + "|" + boneIndex
         );
         this.boneIndex = boneIndex;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          block23: {
              Bone bone;
              float y;
              float x;
              float bx;
              int unused;
              block22: {
                  bone = skeleton.bones.get(this.boneIndex);
                  if (!bone.active) {
                      return;
                  }
                  float[] frames = this.frames;
                  if (time < frames[0]) {
                      switch (blend) {
                          case MixBlend.SETUP: {
                              bone.scaleX = bone.data.scaleX;
                              bone.scaleY = bone.data.scaleY;
                              return;
                          }
                          case MixBlend.FIRST: {
                              bone.scaleX += (bone.data.scaleX - bone.scaleX) * alpha;
                              bone.scaleY += (bone.data.scaleY - bone.scaleY) * alpha;
                          }
                      }
                      return;
                  }
                  int i = ScaleTimeline.search(frames, time, 3);
                  int curveType = (int)this.PROPERTY_IDS[i / 3];
                  switch (curveType) {
                      case 0: {
                          bx = frames[i];
                          x = frames[i + 1];
                          y = frames[i + 2];
                          time = (time - bx) / (frames[i + 3] - bx);
                          x += (frames[i + 3 + 1] - x) * time;
                          y += (frames[i + 3 + 2] - y) * time;
                          break;
                      }
                      case 1: {
                          x = frames[i + 1];
                          y = frames[i + 2];
                          break;
                      }
                      default: {
                          x = this.getBezierValue(time, i, 1, curveType - 2);
                          y = this.getBezierValue(time, i, 2, curveType + 18 - 2);
                      }
                  }
                  x *= bone.data.scaleX;
                  y *= bone.data.scaleY;
                  if (alpha == 1.0f) {
                      if (blend == MixBlend.ADD) {
                          bone.scaleX += x - bone.data.scaleX;
                          bone.scaleY += y - bone.data.scaleY;
                          return;
                      }
                      bone.scaleX = x;
                      bone.scaleY = y;
                      return;
                  }
                  if (direction != MixDirection.OUT) break block22;
                  switch (blend) {
                      case MixBlend.SETUP: {
                          bx = bone.data.scaleX;
                          time = bone.data.scaleY;
                          bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bx) * alpha;
                          bone.scaleY = time + (Math.abs(y) * Math.signum(time) - time) * alpha;
                          break block23;
                      }
                      case MixBlend.FIRST: 
                      case MixBlend.REPLACE: {
                          bx = bone.scaleX;
                          time = bone.scaleY;
                          bone.scaleX = bx + (Math.abs(x) * Math.signum(bx) - bx) * alpha;
                          bone.scaleY = time + (Math.abs(y) * Math.signum(time) - time) * alpha;
                          break block23;
                      }
                      case MixBlend.ADD: {
                          bone.scaleX += (x - bone.data.scaleX) * alpha;
                          bone.scaleY += (y - bone.data.scaleY) * alpha;
                      }
                      default: {
                          return;
                      }
                  }
              }
              switch (blend) {
                  case MixBlend.SETUP: {
                      bx = Math.abs(bone.data.scaleX) * Math.signum(x);
                      time = Math.abs(bone.data.scaleY) * Math.signum(y);
                      bone.scaleX = bx + (x - bx) * alpha;
                      bone.scaleY = time + (y - time) * alpha;
                      return;
                  }
                  case MixBlend.FIRST: 
                  case MixBlend.REPLACE: {
                      bx = Math.abs(bone.scaleX) * Math.signum(x);
                      time = Math.abs(bone.scaleY) * Math.signum(y);
                      bone.scaleX = bx + (x - bx) * alpha;
                      bone.scaleY = time + (y - time) * alpha;
                      return;
                  }
                  case MixBlend.ADD: {
                      bone.scaleX += (x - bone.data.scaleX) * alpha;
                      bone.scaleY += (y - bone.data.scaleY) * alpha;
                  }
              }
          }
      }
   }

   public static class ScaleXTimeline extends Animation.CurveTimeline1 {
      public int boneIndex;

      public ScaleXTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.SCALE_X - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.scaleX = this.getScaleValue(time, alpha, blend, direction, bone.scaleX, bone.data.scaleX);
         }
      }
   }

   public static class ScaleYTimeline extends Animation.CurveTimeline1 {
      public int boneIndex;

      public ScaleYTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.SCALE_Y - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.scaleY = this.getScaleValue(time, alpha, blend, direction, bone.scaleY, bone.data.scaleY);
         }
      }
   }

   public static class SequenceTimeline extends Animation.Timeline {
      public int PROPERTY_IDS;
      public HasTextureRegion attachment;

      public SequenceTimeline(int frameCount, int slotIndex, Attachment attachment) {
         super(frameCount, new String[]{Animation.Property.SEQUENCE
            - 1
            + "|"
            + slotIndex
            + "|"
            + ((HasTextureRegion)attachment).getSequence().getId()});
         this.PROPERTY_IDS = slotIndex;
         this.attachment = (HasTextureRegion)attachment;
      }

      @Override
      public final int getFrameEntries() {
         return 3;
      }

      public final void setFrame(int frame, float time, int mode, int index, float delay) {
         frame *= 3;
         super.frames[frame] = time;
         int modeSlot = frame + 1;
         int modeOrdinal = mode - 1;
         super.frames[modeSlot] = (float)(modeOrdinal | index << 4);
         super.frames[frame + 2] = delay;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          Slot slot = skeleton.slots.get(this.PROPERTY_IDS);
          if (!slot.bone.active) {
              return;
          }
          Attachment slotAttachment = slot.attachment;
          if (!(slotAttachment == this.attachment || slotAttachment instanceof VertexAttachment && ((VertexAttachment)slotAttachment).getTimelineAttachment() == this.attachment)) {
              return;
          }
          Sequence sequence = ((HasTextureRegion)slotAttachment).getSequence();
          if (sequence == null) {
              return;
          }
          if (direction == MixDirection.OUT) {
              if (blend == MixBlend.SETUP) {
                  slot.sequenceIndex = -1;
              }
              return;
          }
          float[] frames = this.frames;
          if (time < frames[0]) {
              if (blend == MixBlend.SETUP || blend == MixBlend.FIRST) {
                  slot.sequenceIndex = -1;
              }
              return;
          }
          int index = SequenceTimeline.search(frames, time, 3);
          float before = frames[index];
          direction = (int)frames[index + 1];
          float delay = frames[index + 2];
          index = direction >> 4;
          int count = sequence.getRegions().length;
          if ((direction = Sequence.SequenceMode.VALUES[direction & 15]) != Sequence.SequenceMode.HOLD) {
              index = (int)((float)index + ((time - before) / delay + 1.0E-4f));
              switch (direction) {
                  case Sequence.SequenceMode.ONCE: {
                      index = Math.min(count - 1, index);
                      break;
                  }
                  case Sequence.SequenceMode.LOOP: {
                      index %= count;
                      break;
                  }
                  case Sequence.SequenceMode.PINGPONG: {
                      int n = (count << 1) - 2;
                      index = n == 0 ? 0 : index % n;
                      if (index < count) break;
                      index = n - index;
                      break;
                  }
                  case Sequence.SequenceMode.ONCE_REVERSE: {
                      index = Math.max(count - 1 - index, 0);
                      break;
                  }
                  case Sequence.SequenceMode.LOOP_REVERSE: {
                      index = count - 1 - index % count;
                      break;
                  }
                  case Sequence.SequenceMode.PINGPONG_REVERSE: {
                      int n = (count << 1) - 2;
                      index = n == 0 ? 0 : (index + count - 1) % n;
                      if (index < count) break;
                      index = n - index;
                  }
              }
          }
          slot.sequenceIndex = index;
      }
   }

   public static class ShearTimeline extends Animation.CurveTimeline2 {
      public int boneIndex;

      public ShearTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(
            frameCount,
            bezierCount,
            Animation.Property.SHEAR_X - 1 + "|" + boneIndex,
            Animation.Property.SHEAR_Y - 1 + "|" + boneIndex
         );
         this.boneIndex = boneIndex;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float y;
          float x;
          Bone bone = skeleton.bones.get(this.boneIndex);
          if (!bone.active) {
              return;
          }
          float[] frames = this.frames;
          if (time < frames[0]) {
              switch (blend) {
                  case MixBlend.SETUP: {
                      bone.shearX = bone.data.shearX;
                      bone.shearY = bone.data.shearY;
                      return;
                  }
                  case MixBlend.FIRST: {
                      bone.shearX += (bone.data.shearX - bone.shearX) * alpha;
                      bone.shearY += (bone.data.shearY - bone.shearY) * alpha;
                  }
              }
              return;
          }
          int i = ShearTimeline.search(frames, time, 3);
          direction = (int)this.PROPERTY_IDS[i / 3];
          switch (direction) {
              case 0: {
                  float before = frames[i];
                  x = frames[i + 1];
                  y = frames[i + 2];
                  time = (time - before) / (frames[i + 3] - before);
                  x += (frames[(i + 3) + 1] - x) * time;
                  y += (frames[i + 3 + 2] - y) * time;
                  break;
              }
              case 1: {
                  x = frames[i + 1];
                  y = frames[i + 2];
                  break;
              }
              default: {
                  x = this.getBezierValue(time, i, 1, direction - 2);
                  y = this.getBezierValue(time, i, 2, direction + 18 - 2);
              }
          }
          switch (blend) {
              case MixBlend.SETUP: {
                  bone.shearX = bone.data.shearX + x * alpha;
                  bone.shearY = bone.data.shearY + y * alpha;
                  return;
              }
              case MixBlend.FIRST: 
              case MixBlend.REPLACE: {
                  bone.shearX += (bone.data.shearX + x - bone.shearX) * alpha;
                  bone.shearY += (bone.data.shearY + y - bone.shearY) * alpha;
                  return;
              }
              case MixBlend.ADD: {
                  bone.shearX += x * alpha;
                  bone.shearY += y * alpha;
              }
          }
      }
   }

   public static class ShearXTimeline extends Animation.CurveTimeline1 {
      public int boneIndex;

      public ShearXTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.SHEAR_X - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.shearX = this.getRelativeValue(time, alpha, blend, bone.shearX, bone.data.shearX);
         }
      }
   }

   public static class ShearYTimeline extends Animation.CurveTimeline1 {
      public int boneIndex;

      public ShearYTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.SHEAR_Y - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.shearY = this.getRelativeValue(time, alpha, blend, bone.shearY, bone.data.shearY);
         }
      }
   }

   public abstract static class Timeline {
      public final String[] PROPERTY_IDS;
      public final float[] frames;

      public Timeline(int frameCount, String... propertyIds) {
         if (propertyIds == null) {
            throw new IllegalArgumentException("propertyIds cannot be null.");
         } else {
            this.PROPERTY_IDS = propertyIds;
            this.frames = new float[frameCount * this.getFrameEntries()];
         }
      }

      public final String[] getPropertyIds() {
         return this.PROPERTY_IDS;
      }

      public int getFrameEntries() {
         return 1;
      }

      public int getFrameCount() {
         return this.frames.length / this.getFrameEntries();
      }

      public final float getDuration() {
         return this.frames[this.frames.length - this.getFrameEntries()];
      }

      public abstract void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction);

      public static int search(float[] frames, float time) {
         int n = frames.length;

         for (int i = 1; i < n; i++) {
            if (frames[i] > time) {
               return i - 1;
            }
         }

         return n - 1;
      }

      public static int search(float[] frames, float time, int step) {
         int n = frames.length;

         for (int i = step; i < n; i += step) {
            if (frames[i] > time) {
               return i - step;
            }
         }

         return n - step;
      }

   }

   public static class TransformConstraintTimeline extends Animation.CurveTimeline {
      public int constraintIndex;

      public TransformConstraintTimeline(int frameCount, int bezierCount, int constraintIndex) {
         super(frameCount, bezierCount, new String[]{Animation.Property.TRANSFORM_CONSTRAINT - 1 + "|" + constraintIndex});
         this.constraintIndex = constraintIndex;
      }

      @Override
      public final int getFrameEntries() {
         return 7;
      }

      public final void setFrame(int frame, float time, float mixRotate, float mixX, float mixY, float mixScaleX, float mixScaleY, float mixShearY) {
         frame *= 7;
         super.frames[frame] = time;
         super.frames[frame + 1] = mixRotate;
         super.frames[frame + 2] = mixX;
         super.frames[frame + 3] = mixY;
         super.frames[frame + 4] = mixScaleX;
         super.frames[frame + 5] = mixScaleY;
         super.frames[frame + 6] = mixShearY;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float shearY;
          float scaleY;
          float scaleX;
          float y;
          float x;
          float rotate;
          TransformConstraint constraint = skeleton.transformConstraints.get(this.constraintIndex);
          if (!constraint.active) {
              return;
          }
          float[] frames = this.frames;
          if (time < frames[0]) {
              TransformConstraintData data = constraint.data;
              switch (blend) {
                  case MixBlend.SETUP: {
                      constraint.mixRotate = data.mixRotate;
                      constraint.mixX = data.mixX;
                      constraint.mixY = data.mixY;
                      constraint.mixScaleX = data.mixScaleX;
                      constraint.mixScaleY = data.mixScaleY;
                      constraint.mixShearY = data.mixShearY;
                      return;
                  }
                  case MixBlend.FIRST: {
                      constraint.mixRotate += (data.mixRotate - constraint.mixRotate) * alpha;
                      constraint.mixX += (data.mixX - constraint.mixX) * alpha;
                      constraint.mixY += (data.mixY - constraint.mixY) * alpha;
                      constraint.mixScaleX += (data.mixScaleX - constraint.mixScaleX) * alpha;
                      constraint.mixScaleY += (data.mixScaleY - constraint.mixScaleY) * alpha;
                      constraint.mixShearY += (data.mixShearY - constraint.mixShearY) * alpha;
                  }
              }
              return;
          }
          int i = TransformConstraintTimeline.search(frames, time, 7);
          int curveType = (int)this.PROPERTY_IDS[i / 7];
          switch (curveType) {
              case 0: {
                  float before = frames[i];
                  rotate = frames[i + 1];
                  x = frames[i + 2];
                  y = frames[i + 3];
                  scaleX = frames[i + 4];
                  scaleY = frames[i + 5];
                  shearY = frames[i + 6];
                  time = (time - before) / (frames[i + 7] - before);
                  rotate += (frames[i + 7 + 1] - rotate) * time;
                  x += (frames[i + 7 + 2] - x) * time;
                  y += (frames[i + 7 + 3] - y) * time;
                  scaleX += (frames[i + 7 + 4] - scaleX) * time;
                  scaleY += (frames[i + 7 + 5] - scaleY) * time;
                  shearY += (frames[i + 7 + 6] - shearY) * time;
                  break;
              }
              case 1: {
                  rotate = frames[i + 1];
                  x = frames[i + 2];
                  y = frames[i + 3];
                  scaleX = frames[i + 4];
                  scaleY = frames[i + 5];
                  shearY = frames[i + 6];
                  break;
              }
              default: {
                  rotate = this.getBezierValue(time, i, 1, curveType - 2);
                  x = this.getBezierValue(time, i, 2, curveType + 18 - 2);
                  y = this.getBezierValue(time, i, 3, (curveType + 36) - 2);
                  scaleX = this.getBezierValue(time, i, 4, (curveType + 54) - 2);
                  scaleY = this.getBezierValue(time, i, 5, curveType + 72 - 2);
                  shearY = this.getBezierValue(time, i, 6, curveType + 90 - 2);
              }
          }
          if (blend == MixBlend.SETUP) {
              TransformConstraintData data = constraint.data;
              constraint.mixRotate = data.mixRotate + (rotate - data.mixRotate) * alpha;
              constraint.mixX = data.mixX + (x - data.mixX) * alpha;
              constraint.mixY = data.mixY + (y - data.mixY) * alpha;
              constraint.mixScaleX = data.mixScaleX + (scaleX - data.mixScaleX) * alpha;
              constraint.mixScaleY = data.mixScaleY + (scaleY - data.mixScaleY) * alpha;
              constraint.mixShearY = data.mixShearY + (shearY - data.mixShearY) * alpha;
              return;
          }
          constraint.mixRotate += (rotate - constraint.mixRotate) * alpha;
          constraint.mixX += (x - constraint.mixX) * alpha;
          constraint.mixY += (y - constraint.mixY) * alpha;
          constraint.mixScaleX += (scaleX - constraint.mixScaleX) * alpha;
          constraint.mixScaleY += (scaleY - constraint.mixScaleY) * alpha;
          constraint.mixShearY += (shearY - constraint.mixShearY) * alpha;
      }
   }

   public static class TranslateTimeline extends Animation.CurveTimeline2 {
      public int boneIndex;

      public TranslateTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.X - 1 + "|" + boneIndex, Animation.Property.Y - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      // [recovered via CFR from reference bytecode; Vineflower failed on this method]
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
          float y;
          float x;
          Bone bone = skeleton.bones.get(this.boneIndex);
          if (!bone.active) {
              return;
          }
          float[] frames = this.frames;
          if (time < frames[0]) {
              switch (blend) {
                  case MixBlend.SETUP: {
                      bone.x = bone.data.x;
                      bone.y = bone.data.y;
                      return;
                  }
                  case MixBlend.FIRST: {
                      bone.x += (bone.data.x - bone.x) * alpha;
                      bone.y += (bone.data.y - bone.y) * alpha;
                  }
              }
              return;
          }
          int i = TranslateTimeline.search(frames, time, 3);
          direction = (int)this.PROPERTY_IDS[i / 3];
          switch (direction) {
              case 0: {
                  float before = frames[i];
                  x = frames[i + 1];
                  y = frames[i + 2];
                  time = (time - before) / (frames[i + 3] - before);
                  x += (frames[i + 3 + 1] - x) * time;
                  y += (frames[i + 3 + 2] - y) * time;
                  break;
              }
              case 1: {
                  x = frames[i + 1];
                  y = frames[i + 2];
                  break;
              }
              default: {
                  x = this.getBezierValue(time, i, 1, direction - 2);
                  y = this.getBezierValue(time, i, 2, direction + 18 - 2);
              }
          }
          switch (blend) {
              case MixBlend.SETUP: {
                  bone.x = bone.data.x + x * alpha;
                  bone.y = bone.data.y + y * alpha;
                  return;
              }
              case MixBlend.FIRST: 
              case MixBlend.REPLACE: {
                  bone.x += (bone.data.x + x - bone.x) * alpha;
                  bone.y += (bone.data.y + y - bone.y) * alpha;
                  return;
              }
              case MixBlend.ADD: {
                  bone.x += x * alpha;
                  bone.y += y * alpha;
              }
          }
      }
   }

   public static class TranslateXTimeline extends Animation.CurveTimeline1 {
      public int boneIndex;

      public TranslateXTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.X - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.x = this.getRelativeValue(time, alpha, blend, bone.x, bone.data.x);
         }
      }
   }

   public static class TranslateYTimeline extends Animation.CurveTimeline1 {
      public int boneIndex;

      public TranslateYTimeline(int frameCount, int bezierCount, int boneIndex) {
         super(frameCount, bezierCount, Animation.Property.Y - 1 + "|" + boneIndex);
         this.boneIndex = boneIndex;
      }

      @Override
      public final void apply(Skeleton skeleton, float lastTime, float time, @Null Array<Event> events, float alpha, int blend, int direction) {
         Bone bone;
         if ((bone = skeleton.bones.get(this.boneIndex)).active) {
            bone.y = this.getRelativeValue(time, alpha, blend, bone.y, bone.data.y);
         }
      }
   }
}
