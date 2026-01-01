package com.tlnt.world.skeleton;

import com.badlogic.gdx.utils.Array;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Event;

public final class SkeletonEventForwarder extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.skeleton.SpineSkeletonComponent skeletonComponent;
   public SkeletonEventForwarder(SpineSkeletonComponent skeletonComponent) {
      super();
      this.skeletonComponent = skeletonComponent;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (this.skeletonComponent.lastTemplate != null && this.skeletonComponent.lastTemplate.getName().equals(entry.getAnimation().getName())) {
         Array.ArrayIterator iterator = this.skeletonComponent.animationListeners.iterator();

         while (iterator.hasNext()) {
            ((AnimationState.AnimationStateListener)iterator.next()).complete(entry);
         }
      }
   }

   @Override
   public final void event(AnimationState.TrackEntry entry, Event event) {
      if (this.skeletonComponent.lastTemplate != null && this.skeletonComponent.lastTemplate.getName().equals(entry.getAnimation().getName())) {
         Array.ArrayIterator iterator = this.skeletonComponent.animationListeners.iterator();

         while (iterator.hasNext()) {
            ((AnimationState.AnimationStateListener)iterator.next()).event(entry, event);
         }
      }
   }
}
