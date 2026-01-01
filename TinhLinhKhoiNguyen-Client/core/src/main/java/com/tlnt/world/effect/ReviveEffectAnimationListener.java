package com.tlnt.world.effect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class ReviveEffectAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.ReviveEffect effect;
   public ReviveEffectAnimationListener(ReviveEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      ReviveEffect reviveEffect = this.effect;
      this.effect.setEnabled(false);
   }
}
