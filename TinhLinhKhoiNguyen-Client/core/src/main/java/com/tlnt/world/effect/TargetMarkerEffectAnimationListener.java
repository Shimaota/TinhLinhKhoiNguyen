package com.tlnt.world.effect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class TargetMarkerEffectAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.TargetMarkerEffect targetMarkerEffect;
   public TargetMarkerEffectAnimationListener(TargetMarkerEffect effect) {
      super();
      this.targetMarkerEffect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      TargetMarkerEffect markerEffect = this.targetMarkerEffect;
      this.targetMarkerEffect.setEnabled(false);
   }
}
