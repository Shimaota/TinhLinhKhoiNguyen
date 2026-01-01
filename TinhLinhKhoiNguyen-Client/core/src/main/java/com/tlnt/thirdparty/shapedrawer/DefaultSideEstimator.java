package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.MathUtils;

public final class DefaultSideEstimator implements SideEstimator {
   public int minSides;
   public int maxSides;
   public float quality;

   public DefaultSideEstimator() {
      this(20, 4000, 1.0F);
   }

   public DefaultSideEstimator(int minSides, int maxSides, float quality) {
      this.minSides = 20;
      this.maxSides = 4000;
      this.quality = 1.0F;
   }

   @Override
   public final int estimateSidesRequired(float pixelSize, float radiusX, float radiusY) {
      int sides = (int)((float)((float) (Math.PI * 2) * Math.sqrt((double)((radiusX * radiusX + radiusY * radiusY) / 2.0F))) / (16.0F * pixelSize));
      float a = Math.min(radiusX, radiusY);
      radiusX = Math.max(radiusX, radiusY);
      radiusX = (float)Math.sqrt((double)(1.0F - a * a / (radiusX * radiusX)));
      return MathUtils.clamp((int)((float)sides + (float)sides * radiusX * this.quality / 16.0F), this.minSides, this.maxSides);
   }
}
