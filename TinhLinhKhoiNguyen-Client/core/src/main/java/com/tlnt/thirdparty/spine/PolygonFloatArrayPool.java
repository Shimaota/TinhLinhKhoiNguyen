package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;

public final class PolygonFloatArrayPool extends Pool {
   public com.tlnt.thirdparty.spine.SkeletonBounds bounds;
   public PolygonFloatArrayPool(SkeletonBounds bounds) {
      super();
      this.bounds = bounds;
   }

   @Override
   public final Object newObject() {
      return new FloatArray();
   }
}
