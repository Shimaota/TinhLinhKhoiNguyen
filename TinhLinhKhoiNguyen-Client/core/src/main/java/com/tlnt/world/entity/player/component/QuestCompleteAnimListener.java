package com.tlnt.world.entity.player.component;

import com.tlnt.thirdparty.spine.AnimationState;

public final class QuestCompleteAnimListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.entity.player.component.QuestCompleteEffect questCompleteEffect;
   public QuestCompleteAnimListener(QuestCompleteEffect questCompleteEffect) {
      super();
      this.questCompleteEffect = questCompleteEffect;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      this.questCompleteEffect.setEnabled(false);
   }
}
