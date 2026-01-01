package com.tlnt.world.entity.npc.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Event;

public final class NpcSpriteAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.entity.npc.component.NpcSprite plantSprite;
   public NpcSpriteAnimationListener(NpcSprite plantSprite) {
      super();
      this.plantSprite = plantSprite;
   }

   @Override
   public final void event(AnimationState.TrackEntry entry, Event event) {
      if (this.plantSprite.effectStartTime != null) {
         this.plantSprite.effectStartTime.dispatchEventWithCallback(entry.getAnimation().getName(), event);
      }
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (this.plantSprite.effectStartTime != null) {
         this.plantSprite.effectStartTime.dispatchEvent(entry.getAnimation().getName());
      }
   }
}
