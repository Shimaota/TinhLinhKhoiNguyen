package com.tlnt.engine.physics;

import com.badlogic.gdx.math.Vector2;

public enum Direction {
   UP(0.0F, 1.0F),
   UP_RIGHT(1.0F, 1.0F),
   RIGHT(1.0F, 0.0F),
   DOWN_RIGHT(1.0F, -1.0F),
   DOWN(0.0F, -1.0F),
   DOWN_LEFT(-1.0F, -1.0F),
   LEFT(-1.0F, 0.0F),
   UP_LEFT(-1.0F, 1.0F);

   public final Vector2 vector;

   Direction(float x, float y) {
      this.vector = new Vector2(x, y).nor();
   }

}
