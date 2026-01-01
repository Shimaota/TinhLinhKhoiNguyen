package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.mapitem.MapItemEntity;

public final class MapItemFactory {
   public static final ObjectPool<MapItemEntity> MAP_ITEM_POOL = new MapItemPool();

   public static MapItemEntity obtainMapItem(int id, float x, float y) {
      MapItemEntity mapItem;
      (mapItem = MAP_ITEM_POOL.obtain()).setId(id);
      mapItem.teleportTo(x, y);
      return mapItem;
   }

   public static void freeMapItem(MapItemEntity mapItem) {
      MAP_ITEM_POOL.free(mapItem);
   }
}
