package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.fish.AmbientFish;

public final class AmbientFishFactory {
   public static final ObjectPool<AmbientFish> FISH_POOL = new AmbientFishPool();

   public static AmbientFish obtainFish(int id, float x, float y) {
      AmbientFish fish;
      (fish = FISH_POOL.obtain()).setId(id);
      fish.teleportTo(x, y);
      return fish;
   }

   public static void freeFish(AmbientFish fish) {
      FISH_POOL.free(fish);
   }
}
