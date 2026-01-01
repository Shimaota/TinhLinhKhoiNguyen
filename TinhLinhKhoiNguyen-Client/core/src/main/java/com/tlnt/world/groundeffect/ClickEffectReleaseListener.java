package com.tlnt.world.groundeffect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class ClickEffectReleaseListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.groundeffect.ClickEffect effect;
   public ClickEffectReleaseListener(ClickEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      ClickEffect.POOL2.free(this.effect);
   }
}
