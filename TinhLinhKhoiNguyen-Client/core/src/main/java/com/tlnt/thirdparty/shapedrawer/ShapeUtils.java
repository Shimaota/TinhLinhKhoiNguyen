package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.Vector2;

public final class ShapeUtils {
   public static float snapToPixel(float value, float pixelSize, float offset) {
      return (float)Math.round(value / pixelSize) * pixelSize + offset;
   }

   public static boolean epsilonEquals(float a, float b) {
      if (Math.abs(a - b) < 0.001F) {
         return true;
      } else {
         return false;
      }
   }

   public static float angleRad(Vector2 v1, Vector2 v2) {
      return (float)Math.atan2((double)(v2.x * v1.y - v2.y * v1.x), (double)(v1.x * v2.x + v1.y * v2.y));
   }

   static {
      Math.sqrt(2.0);
      Math.sqrt(3.0);
   }
}
