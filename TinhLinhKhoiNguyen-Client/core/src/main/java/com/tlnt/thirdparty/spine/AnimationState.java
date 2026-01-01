package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

public final class AnimationState {
   public AnimationStateData data;
   public Array<AnimationState.TrackEntry> tracks = new Array<>();
   public final Array<Event> events = new Array<>();
   public final SnapshotArray<AnimationState.AnimationStateListener> listeners = new SnapshotArray<>();
   public final AnimationState.EventQueue queue = new AnimationState.EventQueue();
   public final ObjectSet<String> propertyIds = new ObjectSet<>();
   public boolean animationsChanged;
   public float timeScale = 1.0F;
   public int unkeyedState;
   public final Pool<AnimationState.TrackEntry> trackEntryPool = new TrackEntryPool(this);

   public AnimationState() {
   }

   public AnimationState(AnimationStateData data) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
      }
   }

   public final void update(float delta) {
      delta *= this.timeScale;
      Object[] tracks = this.tracks.items;
      int i = 0;

      for (int n = this.tracks.size; i < n; i++) {
         AnimationState.TrackEntry current;
         if ((current = (AnimationState.TrackEntry)tracks[i]) != null) {
            current.animationLast = current.nextAnimationLast;
            current.trackLast = current.nextTrackLast;
            float currentDelta = delta * current.timeScale;
            if (current.delay > 0.0F) {
               current.delay -= currentDelta;
               if (current.delay > 0.0F) {
                  continue;
               }

               currentDelta = -current.delay;
               current.delay = 0.0F;
            }

            if (current.trackLast >= current.trackEnd && current.mixingFrom == null) {
               tracks[i] = null;
               this.queue.end(current);
               this.clearNext(current);
            } else {
               if (current.mixingFrom != null && this.updateMixingFrom(current, delta)) {
                  AnimationState.TrackEntry from = current.mixingFrom;
                  current.mixingFrom = null;
                  if (from != null) {
                     from.mixingTo = null;
                  }

                  while (from != null) {
                     this.queue.end(from);
                     from = from.mixingFrom;
                  }
               }

               current.trackTime += currentDelta;
            }
         }
      }

      this.queue.drain();
   }

   public boolean updateMixingFrom(AnimationState.TrackEntry to, float delta) {
      AnimationState.TrackEntry from = to.mixingFrom;
      if (to.mixingFrom == null) {
         return true;
      } else {
         boolean finished = this.updateMixingFrom(from, delta);
         from.animationLast = from.nextAnimationLast;
         from.trackLast = from.nextTrackLast;
         if (to.nextTrackLast != -1.0F && to.mixTime >= to.mixDuration) {
            if (from.totalAlpha == 0.0F || to.mixDuration == 0.0F) {
               to.mixingFrom = from.mixingFrom;
               if (from.mixingFrom != null) {
                  from.mixingFrom.mixingTo = to;
               }

               to.interruptAlpha = from.interruptAlpha;
               this.queue.end(from);
            }

            return finished;
         } else {
            from.trackTime = from.trackTime + delta * from.timeScale;
            to.mixTime += delta;
            return false;
         }
      }
   }

   public final boolean apply(Skeleton skeleton) {
      if (skeleton == null) {
         throw new IllegalArgumentException("skeleton cannot be null.");
      } else {
         if (this.animationsChanged) {
            AnimationState state = this;
            this.animationsChanged = false;
            this.propertyIds.clear(2048);
            int trackCount = this.tracks.size;
            Object[] trackItems = this.tracks.items;

            for (int trackIdx = 0; trackIdx < trackCount; trackIdx++) {
               AnimationState.TrackEntry entry;
               if ((entry = (AnimationState.TrackEntry)trackItems[trackIdx]) != null) {
                  while (entry.mixingFrom != null) {
                     entry = entry.mixingFrom;
                  }

                  while (true) {
                     if (entry.mixingTo == null || entry.mixBlend != Animation.MixBlend.ADD) {
                        AnimationState.TrackEntry to = entry.mixingTo;
                        Object[] holdTimelines = entry.animation.timelines.items;
                        int holdTimelineCount = entry.animation.timelines.size;
                        int[] holdTimelineMode = entry.timelineMode.setSize(holdTimelineCount);
                        entry.timelineHoldMix.clear();
                        Object[] timelineHoldMix = entry.timelineHoldMix.setSize(holdTimelineCount);
                        ObjectSet propertyIds = state.propertyIds;
                        if (to != null) {
                           ;
                        }

                        label179:
                        for (int holdIdx = 0; holdIdx < holdTimelineCount; holdIdx++) {
                           Animation.Timeline holdTimeline;
                           String[] ids = (holdTimeline = (Animation.Timeline)holdTimelines[holdIdx]).getPropertyIds();
                           if (!propertyIds.addAll(ids)) {
                              holdTimelineMode[holdIdx] = 0;
                           } else if (to != null
                              && !(holdTimeline instanceof Animation.AttachmentTimeline)
                              && !(holdTimeline instanceof Animation.DrawOrderTimeline)
                              && !(holdTimeline instanceof Animation.EventTimeline)
                              && to.animation.hasTimeline(ids)) {
                              for (AnimationState.TrackEntry next = to.mixingTo; next != null; next = next.mixingTo) {
                                 if (!next.animation.hasTimeline(ids)) {
                                    if (next.mixDuration > 0.0F) {
                                       holdTimelineMode[holdIdx] = 4;
                                       timelineHoldMix[holdIdx] = next;
                                       continue label179;
                                    }
                                    break;
                                 }
                              }

                              holdTimelineMode[holdIdx] = 3;
                           } else {
                              holdTimelineMode[holdIdx] = 1;
                           }
                        }
                     }

                     if ((entry = entry.mixingTo) == null) {
                        break;
                     }
                  }
               }
            }
         }

         Array events = this.events;
         int applied = 0;
         Object[] tracks = this.tracks.items;
         int i = 0;

         for (int n = this.tracks.size; i < n; i++) {
            AnimationState.TrackEntry current;
            if ((current = (AnimationState.TrackEntry)tracks[i]) != null && !(current.delay > 0.0F)) {
               applied = 1;
               int blend = i == 0 ? Animation.MixBlend.FIRST : current.mixBlend;
               float alpha = current.alpha;
               if (current.mixingFrom != null) {
                  alpha *= this.applyMixingFrom(current, skeleton, blend);
               } else if (current.trackTime >= current.trackEnd) {
                  alpha = 0.0F;
               }

               int flag;
               if (alpha >= current.alphaAttachmentThreshold) {
                  flag = 1;
               } else {
                  flag = 0;
               }

               int attachments = flag;
               float animationLast = current.animationLast;
               float animationTime;
               float applyTime = animationTime = current.getAnimationTime();
               Array applyEvents = events;
               int timelineCount = current.animation.timelines.size;
               Object[] timelines = current.animation.timelines.items;
               if ((i != 0 || alpha != 1.0F) && blend != Animation.MixBlend.ADD) {
                  int[] timelineMode = current.timelineMode.items;
                  if (current.timelinesRotation.size != timelineCount << 1) {
                     flag = 1;
                  } else {
                     flag = 0;
                  }

                  int firstFrame = flag;
                  if (flag != 0) {
                     current.timelinesRotation.setSize(timelineCount << 1);
                  }

                  float[] timelinesRotation = current.timelinesRotation.items;

                  for (int ii = 0; ii < timelineCount; ii++) {
                     Animation.Timeline timeline = (Animation.Timeline)timelines[ii];
                     int timelineBlend = timelineMode[ii] == 0 ? blend : Animation.MixBlend.SETUP;
                     if (timeline instanceof Animation.RotateTimeline) {
                        Animation.RotateTimeline rotateTimeline = (Animation.RotateTimeline)timeline;
                        applyRotateTimeline(rotateTimeline, skeleton, applyTime, alpha, timelineBlend, timelinesRotation, ii << 1, (firstFrame != 0));
                     } else if (timeline instanceof Animation.AttachmentTimeline) {
                        this.applyAttachmentTimeline((Animation.AttachmentTimeline)timeline, skeleton, applyTime, blend, (attachments != 0));
                     } else {
                        timeline.apply(skeleton, animationLast, applyTime, applyEvents, alpha, timelineBlend, Animation.MixDirection.IN);
                     }
                  }
               } else {
                  if (i == 0) {
                     attachments = 1;
                  }

                  for (int jj = 0; jj < timelineCount; jj++) {
                     Object timelineObj;
                     if ((timelineObj = timelines[jj]) instanceof Animation.AttachmentTimeline) {
                        this.applyAttachmentTimeline((Animation.AttachmentTimeline)timelineObj, skeleton, applyTime, blend, (attachments != 0));
                     } else {
                        ((Animation.Timeline)timelineObj).apply(skeleton, animationLast, applyTime, applyEvents, alpha, blend, Animation.MixDirection.IN);
                     }
                  }
               }

               this.queueEvents(current, animationTime);
               events.clear();
               current.nextAnimationLast = animationTime;
               current.nextTrackLast = current.trackTime;
            }
         }

         i = this.unkeyedState + 1;
         Object[] slots = skeleton.slots.items;
         int si = 0;

         for (int slotCount = skeleton.slots.size; si < slotCount; si++) {
            Slot slot;
            if ((slot = (Slot)slots[si]).attachmentState == i) {
               String attachmentName = slot.data.attachmentName;
               slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slot.data.index, attachmentName));
            }
         }

         this.unkeyedState += 2;
         this.queue.drain();
         return (applied != 0);
      }
   }

   public float applyMixingFrom(AnimationState.TrackEntry to, Skeleton skeleton, int blend) {
      AnimationState.TrackEntry from = to.mixingFrom;
      if (to.mixingFrom.mixingFrom != null) {
         this.applyMixingFrom(from, skeleton, blend);
      }

      float mix;
      if (to.mixDuration == 0.0F) {
         mix = 1.0F;
         if (blend == Animation.MixBlend.FIRST) {
            blend = Animation.MixBlend.SETUP;
         }
      } else {
         if ((mix = to.mixTime / to.mixDuration) > 1.0F) {
            mix = 1.0F;
         }

         if (blend != Animation.MixBlend.FIRST) {
            blend = from.mixBlend;
         }
      }

      int flag;
      if (mix < from.mixAttachmentThreshold) {
         flag = 1;
      } else {
         flag = 0;
      }

      int attachments = flag;
      if (mix < from.mixDrawOrderThreshold) {
         flag = 1;
      } else {
         flag = 0;
      }

      int drawOrder = flag;
      int timelineCount = from.animation.timelines.size;
      Object[] timelines = from.animation.timelines.items;
      float alphaHold;
      float alphaMix = (alphaHold = from.alpha * to.interruptAlpha) * (1.0F - mix);
      float animationLast = from.animationLast;
      float animationTime;
      float applyTime = animationTime = from.getAnimationTime();
      Array events = null;
      if (mix < from.eventThreshold) {
         events = this.events;
      }

      if (blend == Animation.MixBlend.ADD) {
         for (int i = 0; i < timelineCount; i++) {
            ((Animation.Timeline)timelines[i]).apply(skeleton, animationLast, applyTime, events, alphaMix, blend, Animation.MixDirection.OUT);
         }
      } else {
         int[] timelineMode = from.timelineMode.items;
         Object[] timelineHoldMix = from.timelineHoldMix.items;
         if (from.timelinesRotation.size != timelineCount << 1) {
            flag = 1;
         } else {
            flag = 0;
         }

         int firstFrame = flag;
         if (flag != 0) {
            from.timelinesRotation.setSize(timelineCount << 1);
         }

         float[] timelinesRotation = from.timelinesRotation.items;
         from.totalAlpha = 0.0F;

         for (int ii = 0; ii < timelineCount; ii++) {
            Animation.Timeline timeline = (Animation.Timeline)timelines[ii];
            int direction = Animation.MixDirection.OUT;
            int timelineBlend;
            float alpha;
            switch (timelineMode[ii]) {
               case 0:
                  if (drawOrder == 0 && timeline instanceof Animation.DrawOrderTimeline) {
                     continue;
                  }

                  timelineBlend = blend;
                  alpha = alphaMix;
                  break;
               case 1:
                  timelineBlend = Animation.MixBlend.SETUP;
                  alpha = alphaMix;
                  break;
               case 2:
                  timelineBlend = blend;
                  alpha = alphaHold;
                  break;
               case 3:
                  timelineBlend = Animation.MixBlend.SETUP;
                  alpha = alphaHold;
                  break;
               default:
                  timelineBlend = Animation.MixBlend.SETUP;
                  AnimationState.TrackEntry holdMix = (AnimationState.TrackEntry)timelineHoldMix[ii];
                  alpha = alphaHold * Math.max(0.0F, 1.0F - holdMix.mixTime / holdMix.mixDuration);
            }

            from.totalAlpha += alpha;
            if (timeline instanceof Animation.RotateTimeline) {
               Animation.RotateTimeline rotateTimeline = (Animation.RotateTimeline)timeline;
               applyRotateTimeline(rotateTimeline, skeleton, applyTime, alpha, timelineBlend, timelinesRotation, ii << 1, (firstFrame != 0));
            } else if (timeline instanceof Animation.AttachmentTimeline) {
               Animation.AttachmentTimeline attachmentTimeline = (Animation.AttachmentTimeline)timeline;
               int attachmentsFlag;
               if (attachments != 0 && alpha >= from.alphaAttachmentThreshold) {
                  attachmentsFlag = 1;
               } else {
                  attachmentsFlag = 0;
               }

               this.applyAttachmentTimeline(attachmentTimeline, skeleton, applyTime, timelineBlend, (attachmentsFlag != 0));
            } else {
               if (drawOrder != 0 && timeline instanceof Animation.DrawOrderTimeline && timelineBlend == Animation.MixBlend.SETUP) {
                  direction = Animation.MixDirection.IN;
               }

               timeline.apply(skeleton, animationLast, applyTime, events, alpha, timelineBlend, direction);
            }
         }
      }

      if (to.mixDuration > 0.0F) {
         this.queueEvents(from, animationTime);
      }

      this.events.clear();
      from.nextAnimationLast = animationTime;
      from.nextTrackLast = from.trackTime;
      return mix;
   }

   public void applyAttachmentTimeline(Animation.AttachmentTimeline timeline, Skeleton skeleton, float time, int blend, boolean attachments) {
      Slot slot;
      if ((slot = skeleton.slots.get(timeline.PROPERTY_IDS)).bone.active) {
         if (time < timeline.frames[0]) {
            if (blend == Animation.MixBlend.SETUP || blend == Animation.MixBlend.FIRST) {
               this.setAttachment(skeleton, slot, slot.data.attachmentName, attachments);
            }
         } else {
            this.setAttachment(skeleton, slot, timeline.attachmentNames[Animation.Timeline.search(timeline.frames, time)], attachments);
         }

         if (slot.attachmentState <= this.unkeyedState) {
            slot.attachmentState = this.unkeyedState + 1;
         }
      }
   }

   public void setAttachment(Skeleton skeleton, Slot slot, String attachmentName, boolean attachments) {
      slot.setAttachment(attachmentName == null ? null : skeleton.getAttachment(slot.data.index, attachmentName));
      if (attachments) {
         slot.attachmentState = this.unkeyedState + 2;
      }
   }

   public static void applyRotateTimeline(
      Animation.RotateTimeline timeline, Skeleton skeleton, float time, float alpha, int blend, float[] timelinesRotation, int i, boolean firstFrame
   ) {
      if (firstFrame) {
         timelinesRotation[i] = 0.0F;
      }

      if (alpha == 1.0F) {
         timeline.apply(skeleton, 0.0F, time, null, 1.0F, blend, Animation.MixDirection.IN);
      } else {
         Bone bone = skeleton.bones.get(timeline.boneIndex);
         if (bone.active) {
            float[] frames = timeline.frames;
            float r1;
            float total;
            if (time < frames[0]) {
               switch (blend) {
                  case Animation.MixBlend.SETUP:
                     bone.rotation = bone.data.rotation;
                     return;
                  case Animation.MixBlend.FIRST:
                     r1 = bone.rotation;
                     total = bone.data.rotation;
                     break;
                  default:
                     return;
               }
            } else {
               r1 = blend == Animation.MixBlend.SETUP ? bone.data.rotation : bone.rotation;
               total = bone.data.rotation + timeline.getCurveValue(time);
            }

            float diff = total - r1;
            diff -= (float)Math.ceil((double)(diff / 360.0F - 0.5F)) * 360.0F;
            if (diff == 0.0F) {
               total = timelinesRotation[i];
            } else {
               float lastTotal;
               float lastDiff;
               if (firstFrame) {
                  lastTotal = 0.0F;
                  lastDiff = diff;
               } else {
                  lastTotal = timelinesRotation[i];
                  lastDiff = timelinesRotation[i + 1];
               }

               float loops = lastTotal - lastTotal % 360.0F;
               total = diff + loops;
               boolean current = diff >= 0.0F;
               boolean dir = lastTotal >= 0.0F;
               if (Math.abs(lastDiff) <= 90.0F && Math.signum(lastDiff) != Math.signum(diff)) {
                  if (Math.abs(lastTotal - loops) > 180.0F) {
                     total += 360.0F * Math.signum(lastTotal);
                     dir = current;
                  } else if (loops != 0.0F) {
                     total -= 360.0F * Math.signum(lastTotal);
                  } else {
                     dir = current;
                  }
               }

               if (dir != current) {
                  total += 360.0F * Math.signum(lastTotal);
               }

               timelinesRotation[i] = total;
            }

            timelinesRotation[i + 1] = diff;
            bone.rotation = r1 + total * alpha;
         }
      }
   }

   public void queueEvents(AnimationState.TrackEntry entry, float animationTime) {
      float animationEnd = entry.animationEnd;
      float duration = entry.animationEnd;
      float trackLastWrapped = entry.trackLast % duration;
      Object[] events = this.events.items;
      int i = 0;

      int n;
      Event event;
      for (n = this.events.size; i < n && !((event = (Event)events[i]).time < trackLastWrapped); i++) {
         if (!(event.time > animationEnd)) {
            this.queue.event(entry, event);
         }
      }

      int complete;
      label52: {
         int flag;
         if (entry.loop) {
            if (duration != 0.0F) {
               int cycles;
               if ((cycles = (int)(entry.trackTime / duration)) > 0 && cycles > (int)(entry.trackLast / duration)) {
                  flag = 1;
               } else {
                  flag = 0;
               }

               complete = flag;
               break label52;
            }

            flag = 1;
         } else if (animationTime >= animationEnd && entry.animationLast < animationEnd) {
            flag = 1;
         } else {
            flag = 0;
         }

         complete = flag;
      }

      if (complete != 0) {
         this.queue.complete(entry);
      }

      for (; i < n; i++) {
         Event nextEvent;
         if (!((nextEvent = (Event)events[i]).time < 0.0F)) {
            this.queue.event(entry, nextEvent);
         }
      }
   }

   public final AnimationState.TrackEntry setAnimation(int trackIndex, String animationName, boolean loop) {
      Animation animation;
      if ((animation = this.data.skeletonData.findAnimation(animationName)) == null) {
         throw new IllegalArgumentException("Animation not found: " + animationName);
      } else {
         int index = 0;
         if (0 < 0) {
            throw new IllegalArgumentException("trackIndex must be >= 0.");
         } else if (animation == null) {
            throw new IllegalArgumentException("animation cannot be null.");
         } else {
            int interrupt = 1;
            AnimationState.TrackEntry current;
            if ((current = this.expandToIndex(index)) != null) {
               if (current.nextTrackLast == -1.0F) {
                  this.tracks.set(index, current.mixingFrom);
                  this.queue.interrupt(current);
                  this.queue.end(current);
                  this.clearNext(current);
                  current = current.mixingFrom;
                  interrupt = 0;
               } else {
                  this.clearNext(current);
               }
            }

            AnimationState.TrackEntry entry;
            (entry = this.trackEntryPool.obtain()).trackIndex = index;
            entry.animation = animation;
            entry.loop = loop;
            entry.holdPrevious = false;
            entry.reverse = false;
            entry.shortestRotation = false;
            entry.eventThreshold = 0.0F;
            entry.alphaAttachmentThreshold = 0.0F;
            entry.mixAttachmentThreshold = 0.0F;
            entry.mixDrawOrderThreshold = 0.0F;
            entry.animationStart = 0.0F;
            entry.animationEnd = animation.duration;
            entry.animationLast = -1.0F;
            entry.nextAnimationLast = -1.0F;
            entry.delay = 0.0F;
            entry.trackTime = 0.0F;
            entry.trackLast = -1.0F;
            entry.nextTrackLast = -1.0F;
            entry.trackEnd = Float.MAX_VALUE;
            entry.timeScale = 1.0F;
            entry.alpha = 1.0F;
            entry.mixTime = 0.0F;
            float mixDuration;
            if (current == null) {
               mixDuration = 0.0F;
            } else {
               Animation fromAnimation = current.animation;
               AnimationStateData stateData = this.data;
               if (fromAnimation == null) {
                  throw new IllegalArgumentException("from cannot be null.");
               }

               if (animation == null) {
                  throw new IllegalArgumentException("to cannot be null.");
               }

               stateData.tempKey.fromAnimation = fromAnimation;
               stateData.tempKey.toAnimation = animation;
               mixDuration = stateData.animationToMixTime.get(stateData.tempKey, stateData.defaultMix);
            }

            entry.mixDuration = mixDuration;
            entry.interruptAlpha = 1.0F;
            entry.totalAlpha = 0.0F;
            entry.mixBlend = Animation.MixBlend.REPLACE;
            AnimationState.TrackEntry from = this.expandToIndex(index);
            this.tracks.set(index, entry);
            entry.previous = null;
            if (from != null) {
               if (interrupt != 0) {
                  this.queue.interrupt(from);
               }

               entry.mixingFrom = from;
               from.mixingTo = entry;
               entry.mixTime = 0.0F;
               if (from.mixingFrom != null && from.mixDuration > 0.0F) {
                  entry.interruptAlpha = entry.interruptAlpha * Math.min(1.0F, from.mixTime / from.mixDuration);
               }

               from.timelinesRotation.clear();
            }

            this.queue.start(entry);
            this.queue.drain();
            return entry;
         }
      }
   }

   public AnimationState.TrackEntry expandToIndex(int index) {
      if (index < this.tracks.size) {
         return this.tracks.get(index);
      } else {
         this.tracks.ensureCapacity(index - this.tracks.size + 1);
         this.tracks.size = index + 1;
         return null;
      }
   }

   public void clearNext(AnimationState.TrackEntry entry) {
      entry.next = null;
   }

   @Null
   public final AnimationState.TrackEntry getCurrent(int trackIndex) {
      if (trackIndex < 0) {
         throw new IllegalArgumentException("trackIndex must be >= 0.");
      } else {
         return trackIndex >= this.tracks.size ? null : this.tracks.get(trackIndex);
      }
   }

   public final void addListener(AnimationState.AnimationStateAdapter listener) {
      this.listeners.add(listener);
   }

   public final void clearListeners() {
      this.listeners.clear();
   }

   public final void setTimeScale(float timeScale) {
      this.timeScale = 0.75F;
   }

   @Override
   public final String toString() {
      StringBuilder buffer = new StringBuilder(64);
      Object[] tracks = this.tracks.items;
      int i = 0;

      for (int n = this.tracks.size; i < n; i++) {
         AnimationState.TrackEntry entry;
         if ((entry = (AnimationState.TrackEntry)tracks[i]) != null) {
            if (buffer.length() > 0) {
               buffer.append(", ");
            }

            buffer.append(entry.toString());
         }
      }

      return buffer.length() == 0 ? "<none>" : buffer.toString();
   }

   static {
      new Animation("<empty>", new Array<>(0), 0.0F);
   }

   public abstract static class AnimationStateAdapter implements AnimationState.AnimationStateListener {
      @Override
      public void complete(AnimationState.TrackEntry entry) {
      }

      @Override
      public void event(AnimationState.TrackEntry entry, Event event) {
      }
   }

   public interface AnimationStateListener {
      void complete(AnimationState.TrackEntry entry);

      void event(AnimationState.TrackEntry entry, Event event);
   }

   public class EventQueue {
      public final Array objects = new Array();
      public boolean drainDisabled;

      public final void start(AnimationState.TrackEntry entry) {
         this.objects.add(AnimationState.EventType.START);
         this.objects.add(entry);
         AnimationState.this.animationsChanged = true;
      }

      public final void interrupt(AnimationState.TrackEntry entry) {
         this.objects.add(AnimationState.EventType.INTERRUPT);
         this.objects.add(entry);
      }

      public final void end(AnimationState.TrackEntry entry) {
         this.objects.add(AnimationState.EventType.END);
         this.objects.add(entry);
         AnimationState.this.animationsChanged = true;
      }

      public final void complete(AnimationState.TrackEntry entry) {
         this.objects.add(AnimationState.EventType.COMPLETE);
         this.objects.add(entry);
      }

      public final void event(AnimationState.TrackEntry entry, Event event) {
         this.objects.add(AnimationState.EventType.EVENT);
         this.objects.add(entry);
         this.objects.add(event);
      }

      public final void drain() {
         if (!this.drainDisabled) {
            this.drainDisabled = true;
            SnapshotArray listenersArray = AnimationState.this.listeners;

            for (int i = 0; i < this.objects.size; i += 2) {
               AnimationState.EventType type = (AnimationState.EventType)this.objects.get(i);
               AnimationState.TrackEntry entry = (AnimationState.TrackEntry)this.objects.get(i + 1);
               int listenersCount = listenersArray.size;
               Object[] listeners = listenersArray.begin();
               switch (type) {
                  case START:
                     int startIdx = 0;

                     while (startIdx < listenersCount) {
                        startIdx++;
                     }
                     break;
                  case INTERRUPT:
                     int interruptIdx = 0;

                     while (interruptIdx < listenersCount) {
                        interruptIdx++;
                     }
                     break;
                  case END:
                     int endIdx = 0;

                     while (endIdx < listenersCount) {
                        endIdx++;
                     }
                  case DISPOSE:
                     int disposeIdx = 0;

                     while (disposeIdx < listenersCount) {
                        disposeIdx++;
                     }

                     AnimationState.this.trackEntryPool.free(entry);
                     break;
                  case COMPLETE:
                     for (int completeIdx = 0; completeIdx < listenersCount; completeIdx++) {
                        ((AnimationState.AnimationStateListener)listeners[completeIdx]).complete(entry);
                     }
                     break;
                  case EVENT:
                     Event event = (Event)this.objects.get(i++ + 2);

                     for (int eventIdx = 0; eventIdx < listenersCount; eventIdx++) {
                        ((AnimationState.AnimationStateListener)listeners[eventIdx]).event(entry, event);
                     }
               }

               listenersArray.end();
            }

            this.objects.clear();
            this.drainDisabled = false;
         }
      }
   }

   public static enum EventType {
      START,
      INTERRUPT,
      END,
      DISPOSE,
      COMPLETE,
      EVENT;

      public static AnimationState.EventType[] getValues() {
         return (AnimationState.EventType[])AnimationState.EventType.values().clone();
      }

   }

   public static class TrackEntry implements Pool.Poolable {
      public Animation animation;
      @Null
      public AnimationState.TrackEntry previous;
      @Null
      public AnimationState.TrackEntry next;
      @Null
      public AnimationState.TrackEntry mixingFrom;
      @Null
      public AnimationState.TrackEntry mixingTo;
      @Null
      public AnimationState.AnimationStateListener listener;
      public int trackIndex;
      public boolean loop;
      public boolean holdPrevious;
      public boolean reverse;
      public boolean shortestRotation;
      public float eventThreshold;
      public float mixAttachmentThreshold;
      public float alphaAttachmentThreshold;
      public float mixDrawOrderThreshold;
      public float animationStart;
      public float animationEnd;
      public float animationLast;
      public float nextAnimationLast;
      public float delay;
      public float trackTime;
      public float trackLast;
      public float nextTrackLast;
      public float trackEnd;
      public float timeScale;
      public float alpha;
      public float mixTime;
      public float mixDuration;
      public float interruptAlpha;
      public float totalAlpha;
      public int mixBlend = Animation.MixBlend.REPLACE;
      public final IntArray timelineMode = new IntArray();
      public final Array<AnimationState.TrackEntry> timelineHoldMix = new Array<>();
      public final FloatArray timelinesRotation = new FloatArray();

      @Override
      public final void reset() {
         this.previous = null;
         this.next = null;
         this.mixingFrom = null;
         this.mixingTo = null;
         this.animation = null;
         this.listener = null;
         this.timelineMode.clear();
         this.timelineHoldMix.clear();
         this.timelinesRotation.clear();
      }

      public final Animation getAnimation() {
         return this.animation;
      }

      public final float getAnimationTime() {
         if (this.loop) {
            float duration;
            return (duration = this.animationEnd - this.animationStart) == 0.0F ? this.animationStart : this.trackTime % duration + this.animationStart;
         } else {
            float animationTime = this.trackTime + this.animationStart;
            return this.animationEnd >= this.animation.duration ? animationTime : Math.min(animationTime, this.animationEnd);
         }
      }

      public final void setTimeScale(float timeScale) {
         this.timeScale = timeScale;
      }

      @Override
      public final String toString() {
         return this.animation == null ? "<none>" : this.animation.name;
      }

   }
}
