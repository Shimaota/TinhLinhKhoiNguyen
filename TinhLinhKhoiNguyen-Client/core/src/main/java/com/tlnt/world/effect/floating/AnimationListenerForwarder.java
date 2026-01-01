package com.tlnt.world.effect.floating;

import com.badlogic.gdx.utils.Array;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Event;

public final class AnimationListenerForwarder extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.floating.PooledFloatingEffect effect;
   public AnimationListenerForwarder(PooledFloatingEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (this.effect.currentAnimation != null && this.effect.currentAnimation.getName().equals(entry.getAnimation().getName())) {
         Array.ArrayIterator it = this.effect.animationListeners.iterator();

         while (it.hasNext()) {
            ((AnimationState.AnimationStateListener)it.next()).complete(entry);
         }
      }
   }

   @Override
   public final void event(AnimationState.TrackEntry entry, Event event) {
      if (this.effect.currentAnimation != null && this.effect.currentAnimation.getName().equals(entry.getAnimation().getName())) {
         Array.ArrayIterator it = this.effect.animationListeners.iterator();

         while (it.hasNext()) {
            ((AnimationState.AnimationStateListener)it.next()).event(entry, event);
         }
      }
   }
}
