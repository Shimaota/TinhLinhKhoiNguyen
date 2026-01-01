package com.tlnt.world.projectile.thunderbolt.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.world.projectile.thunderbolt.ThunderboltProjectile;

public final class ThunderboltAnimationListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.projectile.thunderbolt.component.ThunderboltSkeletonComponent skeletonComponent;

   public ThunderboltAnimationListener(ThunderboltSkeletonComponent skeletonComponent) {
      super();
      this.skeletonComponent = skeletonComponent;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      if (entry.getAnimation().getName().equals("explosion")) {
         ThunderboltProjectile.free(this.skeletonComponent.projectile2);
      }
   }

}
