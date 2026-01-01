package com.tlnt.world.entity.decoration;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class MapDecorationObjectPool extends Pool<MapDecorationObject> {

   @Override
   public MapDecorationObject newObject() {
      return new com.tlnt.world.entity.decoration.MapDecorationObject(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final MapDecorationObject obtain() {
      MapDecorationObject obj;
      (obj = (MapDecorationObject)super.obtain()).setActive(true);
      return obj;
   }
}
