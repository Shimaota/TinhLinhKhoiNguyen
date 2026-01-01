package com.tlnt.model.player;

public final class MoveSpeedTable {
   public static final int SPEED_NORMAL;
   public static final int SPEED_MOUNT;
   public static final int SPEED_FAST = 3;
   public static final int[] VALUES;

   public static int[] getSpeedValues() {
      return (int[])VALUES.clone();
   }

   static {
      SPEED_NORMAL = 1;
      SPEED_MOUNT = 2;
      int[] values = new int[3];
      values[0] = SPEED_NORMAL;
      values[1] = SPEED_MOUNT;
      values[2] = SPEED_FAST;
      VALUES = values;
   }
}
