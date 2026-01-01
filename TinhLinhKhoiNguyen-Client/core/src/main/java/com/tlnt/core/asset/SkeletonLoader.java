package com.tlnt.core.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.tlnt.core.TaskScheduler;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.SkeletonBinary;
import com.tlnt.thirdparty.spine.SkeletonData;
import com.tlnt.util.FileStorage;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Loads + keeps Spine skeletons by (name, scale). Render (GL) thread only.
 *
 * <p>Memory release (2026-10-19): the original kept EVERY loaded atlas until the game closed — textures are decompressed
 * RGBA at 4 bytes/pixel, each 2048² page = 16 MB; skill effects for 5 weapons ~230 MB, each character outfit ~17 MB, mobs ~5-15 MB.
 * Now each entry counts the (AnimationState, Skeleton) pairs in use; when the total texture size of UNUSED entries exceeds
 * {@link #IDLE_BUDGET_BYTES} the least recently used entry is disposed first (LRU), and entries idle longer than {@link #MAX_IDLE_MS}
 * are dropped too. The first version (drop after 90 s) made walking back and forth between a few maps reload + re-upload textures
 * constantly -> CPU spiked ~20% on every map change; the RAM budget keeps familiar maps while still capping RAM. Callers that
 * only obtain and never release (dialogs, labels...) merely keep that entry alive — safe.
 */
public final class SkeletonLoader {
   public static final ObjectMap<String, SkeletonCacheEntry> skeletonCache = new ObjectMap<>();
   public static final ObjectSet<String> loadingKeys = new ObjectSet<>();
   /** Total texture bytes kept for entries NOBODY is using — above this the least recently used entry is dropped. */
   static final long IDLE_BUDGET_BYTES = 256L * 1024 * 1024;
   /** Entries released more recently than this are kept (map change -> coming straight back). */
   static final long MIN_IDLE_MS = 30_000L;
   /** Entries idle longer than this are dropped even when under the budget. */
   static final long MAX_IDLE_MS = 15 * 60_000L;
   private static final long SWEEP_EVERY_MS = 10_000L;
   /** Pair in use -> its cache entry (double release / releasing an unknown pair = ignored, no miscount). */
   private static final IdentityHashMap<NamedPair<?, ?>, SkeletonCacheEntry> LIVE = new IdentityHashMap<>();
   private static long lastSweepAt;

   public static void init() {
   }

   public static NamedPair<AnimationState, Skeleton> obtainSkeleton(String name, float scale, SkeletonLoader.AnimationMixConfig mixConfig) {
      sweep();
      String cacheKey = name + scale;
      if (!skeletonCache.containsKey(cacheKey) && !loadingKeys.contains(cacheKey)) {
         loadingKeys.add(cacheKey);
         FileHandle atlasFile;
         if ((atlasFile = Gdx.files.internal("skeletons/" + name + ".atlas")).exists()) {
            TextureAtlas atlas = new TextureAtlas(atlasFile);
            SkeletonBinary binary;
            (binary = new SkeletonBinary(atlas)).setScale(scale / 100.0F);
            SkeletonData skeletonData;
            if ((skeletonData = binary.readSkeletonData(Gdx.files.internal("skeletons/" + name + ".skel"))) != null) {
               skeletonCache.put(cacheKey, new SkeletonCacheEntry(skeletonData, mixConfig, atlas));
            } else {
               atlas.dispose();
            }

            loadingKeys.remove(cacheKey);
         } else if ((atlasFile = FileStorage.getExternalFile(name + ".atlas")).exists()) {
            EncryptedTextureAtlas encryptedAtlas = new EncryptedTextureAtlas(atlasFile);
            SkeletonBinary externalBinary;
            (externalBinary = new SkeletonBinary(encryptedAtlas)).setScale(scale / 100.0F);
            TaskScheduler.schedule(new SkeletonLoadTask(externalBinary, name, cacheKey, mixConfig, encryptedAtlas), 0L, TimeUnit.MILLISECONDS);
         }
      }

      SkeletonCacheEntry entry;
      if ((entry = skeletonCache.get(cacheKey)) == null) {
         return null;
      } else {
         NamedPair<AnimationState, Skeleton> pair;
         (pair = new NamedPair<>(name)).first = entry.obtainAnimationState();
         pair.second = entry.obtainSkeleton();
         entry.inUse++;
         LIVE.put(pair, entry);
         return pair;
      }
   }

   /**
    * Returns a pair obtained earlier. Skeleton / AnimationState are not returned to the pool (the original looked up the cache
    * by name without the scale, so they were never actually reused — kept that way so old skins / colors do not leak to another
    * entity); only the user count is decremented.
    */
   public static void releaseSkeleton(NamedPair<AnimationState, Skeleton> pair) {
      if (pair != null) {
         SkeletonCacheEntry entry = LIVE.remove(pair);
         if (entry != null) {
            if (pair.first != null) {
               pair.first.clearListeners();
            }
            if (--entry.inUse <= 0) {
               entry.inUse = 0;
               entry.idleSince = System.currentTimeMillis();
            }
         }
      }
   }

   /**
    * Every {@link #SWEEP_EVERY_MS}: among unused entries (≥ {@link #MIN_IDLE_MS}) — drop those idle longer than {@link #MAX_IDLE_MS}, then
    * if the remaining texture total exceeds {@link #IDLE_BUDGET_BYTES}, drop the least recently used ones until under the budget.
    */
   private static void sweep() {
      long now = System.currentTimeMillis();
      if (now - lastSweepAt < SWEEP_EVERY_MS) {
         return;
      }
      lastSweepAt = now;
      Array<String> idle = null;
      long idleBytes = 0;
      for (ObjectMap.Entry<String, SkeletonCacheEntry> e : skeletonCache.entries()) {
         SkeletonCacheEntry entry = e.value;
         if (entry.inUse == 0 && entry.atlas != null && now - entry.idleSince >= MIN_IDLE_MS) {
            if (idle == null) {
               idle = new Array<>();
            }
            idle.add(e.key);
            idleBytes += entry.textureBytes();
         }
      }
      if (idle == null) {
         return;
      }
      idle.sort((a, b) -> Long.compare(skeletonCache.get(a).idleSince, skeletonCache.get(b).idleSince)); // oldest first
      for (String key : idle) {
         SkeletonCacheEntry entry = skeletonCache.get(key);
         if (entry == null) {
            continue;
         }
         if (idleBytes <= IDLE_BUDGET_BYTES && now - entry.idleSince < MAX_IDLE_MS) {
            break; // from here on newer: keep
         }
         idleBytes -= entry.textureBytes();
         skeletonCache.remove(key);
         entry.dispose();
      }
   }

   /** A background thread finished loading an external .skel file: put it in the cache on the render thread (ObjectMap is not thread-safe). */
   static void loaded(String cacheKey, SkeletonData data, AnimationMixConfig mixConfig, TextureAtlas atlas) {
      Gdx.app.postRunnable(() -> {
         if (data != null) {
            skeletonCache.put(cacheKey, new SkeletonCacheEntry(data, mixConfig, atlas));
            loadingKeys.remove(cacheKey);
         } else if (atlas != null) {
            atlas.dispose(); // broken file: keep the "loading" key like the original -> no endless reload attempts
         }
      });
   }

   public static class AnimationMixConfig {
      public float defaultMix;
      public Array<SkeletonLoader.AnimationMixEntry> mixEntries = new Array<>();

      public AnimationMixConfig() {
         this(0.0F);
      }

      public AnimationMixConfig(float defaultMix) {
         this.defaultMix = defaultMix;
      }
   }

   public static class AnimationMixEntry {
   }
}
