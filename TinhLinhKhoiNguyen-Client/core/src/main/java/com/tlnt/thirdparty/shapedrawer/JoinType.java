package com.tlnt.thirdparty.shapedrawer;

public final class JoinType {
   public static final int NONE = 1;
   public static final int POINTY;
   public static final int SMOOTH = 3;

   static {
      POINTY = 2;
      int[] values = new int[3];
      values[0] = NONE;
      values[1] = POINTY;
      values[2] = SMOOTH;
   }
}
