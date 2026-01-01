package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.core.GameMain;

public final class MapItemPool extends ObjectPool<MapItemEntity> {
   @Override
   public MapItemEntity newObject() {
      return new MapItemEntity(com.tlnt.core.GameMain.instance.getLoginScreen(), 0, 0.0F, 0.0F);
   }

   @Override
   public final MapItemEntity obtain() {
      MapItemEntity mapItem;
      MapItemEntity obtained = mapItem = (MapItemEntity)super.obtain();
      obtained.setActive(true);
      return mapItem;
   }

   @Override
   public final void free(MapItemEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
