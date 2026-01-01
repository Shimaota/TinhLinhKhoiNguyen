package com.tlnt.engine.particle;

import com.badlogic.gdx.graphics.Color;

public final class TintPoint {
   public float position;
   public Color color;
   public boolean deletable;

   public TintPoint(float position, Color color, boolean deletable) {
      this.position = position;
      this.color = color;
      this.deletable = deletable;
   }
}
