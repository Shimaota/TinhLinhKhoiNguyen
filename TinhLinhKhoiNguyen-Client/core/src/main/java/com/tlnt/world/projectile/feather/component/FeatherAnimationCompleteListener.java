package com.tlnt.world.projectile.feather.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.world.projectile.feather.FeatherProjectileEntity;

public final class FeatherAnimationCompleteListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.projectile.feather.component.FeatherSkeletonComponent skeletonComponent;

   public FeatherAnimationCompleteListener(FeatherSkeletonComponent skeletonComponent) {
      super();
      this.skeletonComponent = skeletonComponent;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      String animName = entry.getAnimation().getName();
      if (animName.equals("explosion")) {
         FeatherProjectileEntity.release(this.skeletonComponent.projectile2);
      }
   }

}
