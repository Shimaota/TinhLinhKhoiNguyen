package com.tlnt.world.entity.animal.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Event;

public final class AnimalSpineAnimListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.entity.animal.component.AnimalSprite animalSprite;
   public AnimalSpineAnimListener(AnimalSprite animalSprite) {
      super();
      this.animalSprite = animalSprite;
   }

   @Override
   public final void event(AnimationState.TrackEntry entry, Event event) {
      if (this.animalSprite.effectStartTime != null) {
         this.animalSprite.effectStartTime.dispatchEventWithCallback(entry.getAnimation().getName(), event);
      }
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (this.animalSprite.effectStartTime != null) {
         this.animalSprite.effectStartTime.dispatchEvent(entry.getAnimation().getName());
      }
   }
}
