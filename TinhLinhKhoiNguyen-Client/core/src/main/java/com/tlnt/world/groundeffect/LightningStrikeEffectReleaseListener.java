package com.tlnt.world.groundeffect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class LightningStrikeEffectReleaseListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.groundeffect.LightningStrikeEffect effect;
   public LightningStrikeEffectReleaseListener(LightningStrikeEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      LightningStrikeEffect.POOL2.free(this.effect);
   }
}
