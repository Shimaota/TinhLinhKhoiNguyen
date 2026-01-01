package com.tlnt.world.effect.floating;

import com.tlnt.thirdparty.spine.AnimationState;

public final class AnimationCompletionHandler extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.floating.PooledFloatingEffect effect;
   public AnimationCompletionHandler(PooledFloatingEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      this.effect.isAlive = false;
   }
}
