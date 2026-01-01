package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.fish.AmbientFish;
import com.tlnt.core.GameMain;

public final class AmbientFishPool extends ObjectPool<AmbientFish> {

   @Override
   public AmbientFish newObject() {
      return new com.tlnt.world.entity.fish.AmbientFish(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final AmbientFish obtain() {
      AmbientFish fish;
      AmbientFish obtained = fish = (AmbientFish)super.obtain();
      obtained.setActive(true);
      return fish;
   }

   @Override
   public final void free(AmbientFish obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
