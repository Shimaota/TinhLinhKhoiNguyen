package com.tlnt.core.asset;

public final class PendingSkeletonRequest {
   public boolean pending;
   public String skeletonName;
   public float scale;
   public SkeletonLoader.AnimationMixConfig mixConfig;
   public String[] skinNames;
   public long lastAttemptTime;

   public final void clear() {
      this.pending = false;
      this.skeletonName = null;
      this.mixConfig = null;
      this.skinNames = null;
   }
}
