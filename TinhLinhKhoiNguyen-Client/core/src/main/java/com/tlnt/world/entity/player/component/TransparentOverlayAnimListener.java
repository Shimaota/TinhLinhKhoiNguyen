package com.tlnt.world.entity.player.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Event;

public final class TransparentOverlayAnimListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.entity.player.component.TransparentBodyOverlay transparentBodyOverlay;
   public TransparentOverlayAnimListener(TransparentBodyOverlay transparentBodyOverlay) {
      super();
      this.transparentBodyOverlay = transparentBodyOverlay;
   }

   @Override
   public final void event(AnimationState.TrackEntry entry, Event event) {
      if (this.transparentBodyOverlay.lastTemplate != null) {
         this.transparentBodyOverlay.lastTemplate.dispatchEventWithCallback(entry.getAnimation().getName(), event);
      }
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (this.transparentBodyOverlay.lastTemplate != null) {
         this.transparentBodyOverlay.lastTemplate.dispatchEvent(entry.getAnimation().getName());
      }
   }
}
