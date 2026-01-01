package com.tlnt.world.entity.mob.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Event;

public final class MobSpriteAnimListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.entity.mob.component.MobSprite mobSprite;
   public MobSpriteAnimListener(MobSprite mobSprite) {
      super();
      this.mobSprite = mobSprite;
   }

   @Override
   public final void event(AnimationState.TrackEntry entry, Event event) {
      if (this.mobSprite.effectDuration != null) {
         this.mobSprite.effectDuration.dispatchEventWithCallback(entry.getAnimation().getName(), event);
      }
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (this.mobSprite.effectDuration != null) {
         this.mobSprite.effectDuration.dispatchEvent(entry.getAnimation().getName());
      }
   }
}
