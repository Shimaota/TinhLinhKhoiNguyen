package com.tlnt.world.groundeffect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class SpearProjectileEffectReleaseListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.groundeffect.SpearProjectileEffect effect;
   public SpearProjectileEffectReleaseListener(SpearProjectileEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      SpearProjectileEffect.POOL2.free(this.effect);
   }
}
