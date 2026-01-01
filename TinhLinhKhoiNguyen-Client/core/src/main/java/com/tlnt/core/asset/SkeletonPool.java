package com.tlnt.core.asset;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.thirdparty.spine.Skeleton;

public final class SkeletonPool extends ObjectPool<Skeleton> {
   public SkeletonCacheEntry cacheEntry;

   public SkeletonPool(SkeletonCacheEntry cacheEntry) {
      super();
      this.cacheEntry = cacheEntry;
   }

   @Override
   public Skeleton newObject() {
      return new com.tlnt.thirdparty.spine.Skeleton(this.cacheEntry.skeletonData);
   }
}
