package com.tlnt.world.effect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class IceSpikeEffectAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.IceSpikeEffect effect;

   public IceSpikeEffectAnimationListener(IceSpikeEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      String animName = entry.getAnimation().getName();
      int animIndex = -1;
      switch (animName.hashCode()) {
         case 508081284:
            if (animName.equals("ice_spike_1")) {
               animIndex = 0;
            }
            break;
         case 508081286:
            if (animName.equals("ice_spike_3")) {
               animIndex = 1;
            }
      }

      switch (animIndex) {
         case 0:
            String nextAnimName = "ice_spike_2";
            this.effect.playAnimation(nextAnimName, true, 1.0F);
            return;
         case 1:
            this.effect.stopIceSpike();
      }
   }

}
