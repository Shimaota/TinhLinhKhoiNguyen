package com.tlnt.world.effect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class MobSpawnEffectAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.MobSpawnEffect effect;
   public MobSpawnEffectAnimationListener(MobSpawnEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      MobSpawnEffect spawnEffect = this.effect;
      this.effect.setEnabled(false);
   }
}
