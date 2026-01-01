package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;

public final class PathConstraintData extends ConstraintData {
   public final Array<BoneData> bones = new Array<>();
   public SlotData target;
   public int positionMode;
   public int spacingMode;
   public int rotateMode;
   public float offsetRotation;
   public float position;
   public float spacing;
   public float mixRotate;
   public float mixX;
   public float mixY;

   public PathConstraintData(String name) {
      super(name);
   }

   public static final class PositionMode {
      public static final int FIXED;
      public static final int PERCENT = 2;
      public static final int[] VALUES;

      static {
         FIXED = 1;
         int[] values = new int[2];
         values[0] = FIXED;
         values[1] = PERCENT;
         VALUES = (int[])values.clone();
      }
   }

   public static final class RotateMode {
      public static final int TANGENT = 1;
      public static final int CHAIN;
      public static final int CHAIN_SCALE;
      public static final int[] VALUES;

      static {
         CHAIN = 2;
         CHAIN_SCALE = 3;
         int[] values = new int[3];
         values[0] = TANGENT;
         values[1] = CHAIN;
         values[2] = CHAIN_SCALE;
         VALUES = (int[])values.clone();
      }
   }

   public static final class SpacingMode {
      public static final int LENGTH = 1;
      public static final int FIXED = 2;
      public static final int PERCENT = 3;
      public static final int PROPORTIONAL = 4;
      private static final int[] $VALUES;
      public static final int[] VALUES;

      public static int[] values() {
         return (int[])$VALUES.clone();
      }

      static {
         int[] values = new int[4];
         values[0] = LENGTH;
         values[1] = FIXED;
         values[2] = PERCENT;
         values[3] = PROPORTIONAL;
         $VALUES = values;
         VALUES = values();
      }
   }
}
