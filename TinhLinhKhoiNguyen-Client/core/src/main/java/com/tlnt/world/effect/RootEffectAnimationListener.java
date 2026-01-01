package com.tlnt.world.effect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class RootEffectAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.RootEffect effect;

   public RootEffectAnimationListener(RootEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      String animName = entry.getAnimation().getName();
      int animIndex = -1;
      switch (animName.hashCode()) {
         case -873908090:
            if (animName.equals("tied_1")) {
               animIndex = 0;
            }
            break;
         case -873908088:
            if (animName.equals("tied_3")) {
               animIndex = 1;
            }
      }

      switch (animIndex) {
         case 0:
            this.effect.playAnimation("tied_2", true, 1.0F);
            return;
         case 1:
            this.effect.stopRoot();
      }
   }

}
