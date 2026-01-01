package com.tlnt.thirdparty.spine.utils;

import com.badlogic.gdx.utils.Pool;

public final class PolygonIndicesPool extends Pool {
   public com.tlnt.thirdparty.spine.utils.Triangulator triangulator;
   public PolygonIndicesPool(Triangulator triangulator) {
      super();
      this.triangulator = triangulator;
   }

   @Override
   public final Object newObject() {
      return new com.badlogic.gdx.utils.ShortArray(16);
   }
}
