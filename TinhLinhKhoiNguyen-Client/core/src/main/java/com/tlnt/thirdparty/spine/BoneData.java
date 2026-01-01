package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Null;

public final class BoneData {
   public final int index;
   public final String name;
   @Null
   public final BoneData parent;
   public float length;
   public float x;
   public float y;
   public float rotation;
   public float scaleX = 1.0F;
   public float scaleY = 1.0F;
   public float shearX;
   public float shearY;
   public int inherit = BoneData.Inherit.NORMAL;
   public boolean skinRequired;
   public final Color color = new Color(0.61F, 0.61F, 0.61F, 1.0F);
   @Null
   public String icon;
   public boolean visible;

   public BoneData(int index, String name, @Null BoneData parent) {
      if (index < 0) {
         throw new IllegalArgumentException("index must be >= 0.");
      } else if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else {
         this.index = index;
         this.name = name;
         this.parent = parent;
      }
   }

   @Override
   public final String toString() {
      return this.name;
   }

   public static final class Inherit {
      public static final int NORMAL = 1;
      public static final int ONLY_TRANSLATION = 2;
      public static final int NO_ROTATION_OR_REFLECTION = 3;
      public static final int NO_SCALE = 4;
      public static final int NO_SCALE_OR_REFLECTION = 5;
      private static final int[] VALUES_INTERNAL;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES_INTERNAL.clone();
      }

      static {
         int[] values = new int[5];
         values[0] = NORMAL;
         values[1] = ONLY_TRANSLATION;
         values[2] = NO_ROTATION_OR_REFLECTION;
         values[3] = NO_SCALE;
         values[4] = NO_SCALE_OR_REFLECTION;
         VALUES_INTERNAL = values;
         VALUES = getValues();
      }
   }
}
