package com.tlnt.core.asset;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.thirdparty.spine.AnimationState;

public final class AnimationStatePool extends ObjectPool<AnimationState> {
   public SkeletonCacheEntry cacheEntry;

   public AnimationStatePool(SkeletonCacheEntry cacheEntry) {
      super();
      this.cacheEntry = cacheEntry;
   }

   @Override
   public AnimationState newObject() {
      return new com.tlnt.thirdparty.spine.AnimationState(this.cacheEntry.getAnimationStateData());
   }
}
