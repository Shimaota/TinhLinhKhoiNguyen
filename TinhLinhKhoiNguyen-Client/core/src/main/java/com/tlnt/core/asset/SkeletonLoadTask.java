package com.tlnt.core.asset;

import com.tlnt.thirdparty.spine.SkeletonBinary;
import com.tlnt.thirdparty.spine.SkeletonData;
import com.tlnt.util.FileStorage;

public final class SkeletonLoadTask implements Runnable {
   public com.tlnt.core.asset.SkeletonLoader.AnimationMixConfig mixConfig;
   public java.lang.String cacheKey;
   public java.lang.String skeletonName;
   public com.tlnt.thirdparty.spine.SkeletonBinary skeletonBinary;
   public com.badlogic.gdx.graphics.g2d.TextureAtlas atlas;

   public SkeletonLoadTask(SkeletonBinary skeletonBinary, String skeletonName, String cacheKey, SkeletonLoader.AnimationMixConfig mixConfig,
         com.badlogic.gdx.graphics.g2d.TextureAtlas atlas) {
      this(skeletonBinary, skeletonName, cacheKey, mixConfig);
      this.atlas = atlas;
   }

   public SkeletonLoadTask(SkeletonBinary skeletonBinary, String skeletonName, String cacheKey, SkeletonLoader.AnimationMixConfig mixConfig) {
      super();
      this.skeletonBinary = skeletonBinary;
      this.skeletonName = skeletonName;
      this.cacheKey = cacheKey;
      this.mixConfig = mixConfig;
   }

   @Override
   public final void run() {
      SkeletonData skeletonData = this.skeletonBinary.readSkeletonData(FileStorage.getExternalFile(this.skeletonName + ".skel"));
      SkeletonLoader.loaded(this.cacheKey, skeletonData, this.mixConfig, this.atlas); // into the cache on the render thread
   }
}
