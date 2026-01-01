package com.tlnt.engine.physics.gizmo;

public final class GizmoAxis {
   public static final int NONE;
   public static final int AXIS_XY;
   public static final int AXIS_Y;
   public static final int AXIS_X = 4;

   static {
      NONE = 1;
      AXIS_XY = 2;
      AXIS_Y = 3;
      int[] values = new int[4];
      values[0] = NONE;
      values[1] = AXIS_XY;
      values[2] = AXIS_Y;
      values[3] = AXIS_X;
   }
}
