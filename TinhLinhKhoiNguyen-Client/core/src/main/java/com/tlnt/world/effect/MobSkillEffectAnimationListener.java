package com.tlnt.world.effect;

import com.tlnt.thirdparty.spine.AnimationState;

public final class MobSkillEffectAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.effect.MobSkillEffect effect;
   public MobSkillEffectAnimationListener(MobSkillEffect effect) {
      super();
      this.effect = effect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      MobSkillEffect skillEffect = this.effect;
      this.effect.setEnabled(false);
   }
}
