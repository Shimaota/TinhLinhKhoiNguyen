package com.tlnt.engine.physics;

public final class BodyType {
   public static final int STATIC = 1;
   public static final int KINEMATIC = 2;
   public static final int DYNAMIC = 3;
   public static final int[] VALUES;

   public static int[] values() {
      return (int[])VALUES.clone();
   }

   static {
      int[] values = new int[3];
      values[0] = STATIC;
      values[1] = KINEMATIC;
      values[2] = DYNAMIC;
      VALUES = values;
   }
}
