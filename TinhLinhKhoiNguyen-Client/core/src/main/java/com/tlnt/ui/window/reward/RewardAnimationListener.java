package com.tlnt.ui.window.reward;

import com.tlnt.thirdparty.spine.AnimationState;

public final class RewardAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.ui.window.reward.RewardPopup rewardPopup;

   public RewardAnimationListener(RewardPopup popup) {
      super();
      this.rewardPopup = popup;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (entry.getAnimation().getName().equals("appear")) {
         String idleAnim = "idle";
         this.rewardPopup.playSkeletonAnimation(idleAnim, true);
      }
   }

}
