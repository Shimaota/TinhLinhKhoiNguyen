package com.tlnt.world.groundeffect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class TeleportOutEffectReleaseListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.groundeffect.TeleportOutEffect effect;
   public TeleportOutEffectReleaseListener(TeleportOutEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      TeleportOutEffect.POOL2.free(this.effect);
   }
}
