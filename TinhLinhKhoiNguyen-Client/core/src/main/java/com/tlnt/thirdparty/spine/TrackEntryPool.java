package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Pool;

public final class TrackEntryPool extends Pool {
   public com.tlnt.thirdparty.spine.AnimationState animationState;
   public TrackEntryPool(AnimationState animationState) {
      super();
      this.animationState = animationState;
   }

   @Override
   public final Object newObject() {
      return new AnimationState.TrackEntry();
   }
}
