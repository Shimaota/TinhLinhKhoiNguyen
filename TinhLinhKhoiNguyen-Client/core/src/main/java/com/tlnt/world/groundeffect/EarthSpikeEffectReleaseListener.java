package com.tlnt.world.groundeffect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class EarthSpikeEffectReleaseListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.groundeffect.EarthSpikeEffect effect;
   public EarthSpikeEffectReleaseListener(EarthSpikeEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      EarthSpikeEffect.POOL2.free(this.effect);
   }
}
