package com.tlnt.core.asset;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.AnimationStateData;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.SkeletonData;

public final class SkeletonCacheEntry {
   public final ObjectPool<Skeleton> skeletonPool = new SkeletonPool(this);
   public final ObjectPool<AnimationState> animationStatePool = new AnimationStatePool(this);
   public SkeletonData skeletonData;
   public AnimationStateData animationStateData;
   public SkeletonLoader.AnimationMixConfig mixConfig;

   /** This skeleton's atlas (textures) — disposed when the entry is evicted from the cache ({@code SkeletonLoader.sweep}). */
   public com.badlogic.gdx.graphics.g2d.TextureAtlas atlas;
   /** Number of (AnimationState, Skeleton) pairs in use; 0 since {@link #idleSince}. */
   public int inUse;
   public long idleSince = System.currentTimeMillis();

   public SkeletonCacheEntry(SkeletonData skeletonData, SkeletonLoader.AnimationMixConfig mixConfig) {
      this.skeletonData = skeletonData;
      this.mixConfig = mixConfig;
   }

   public SkeletonCacheEntry(SkeletonData skeletonData, SkeletonLoader.AnimationMixConfig mixConfig,
         com.badlogic.gdx.graphics.g2d.TextureAtlas atlas) {
      this(skeletonData, mixConfig);
      this.atlas = atlas;
   }

   private long textureBytes = -1;

   /** Estimated texture RAM of the atlas (width × height × 4 bytes per page). */
   public long textureBytes() {
      if (this.textureBytes < 0) {
         long sum = 0;
         if (this.atlas != null) {
            for (com.badlogic.gdx.graphics.Texture t : this.atlas.getTextures()) {
               sum += (long)t.getWidth() * t.getHeight() * 4L;
            }
         }
         this.textureBytes = sum;
      }
      return this.textureBytes;
   }

   /** Frees the textures + drops pooled objects (render thread). */
   public void dispose() {
      this.skeletonPool.freeObjects.clear();
      this.animationStatePool.freeObjects.clear();
      if (this.atlas != null) {
         this.atlas.dispose();
         this.atlas = null;
      }
      this.skeletonData = null;
      this.animationStateData = null;
   }

   public AnimationStateData getAnimationStateData() {
      if (this.animationStateData == null) {
         this.animationStateData = new AnimationStateData(this.skeletonData);
         if (this.mixConfig != null) {
            this.animationStateData.setDefaultMix(this.mixConfig.defaultMix);
            Array.ArrayIterator it = this.mixConfig.mixEntries.iterator();
            if (it.hasNext()) {
               SkeletonLoader.AnimationMixEntry mixEntry = (SkeletonLoader.AnimationMixEntry)it.next();
               throw null;
            }
         }
      }

      return this.animationStateData;
   }

   public final Skeleton obtainSkeleton() {
      return this.skeletonPool.obtain();
   }

   public final AnimationState obtainAnimationState() {
      return this.animationStatePool.obtain();
   }

   public final void freeSkeleton(Skeleton skeleton) {
      if (skeleton != null) {
         this.skeletonPool.free(skeleton);
      }
   }

   public final void freeAnimationState(AnimationState state) {
      if (state != null) {
         state.clearListeners();
         this.animationStatePool.free(state);
      }
   }
}
