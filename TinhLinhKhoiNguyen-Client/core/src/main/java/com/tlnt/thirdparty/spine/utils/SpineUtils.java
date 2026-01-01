package com.tlnt.thirdparty.spine.utils;

import java.lang.reflect.Array;

public final class SpineUtils {

   public static float cosDeg(float degrees) {
      return (float)Math.cos((double)(degrees * (float) (Math.PI / 180.0)));
   }

   public static float sinDeg(float degrees) {
      return (float)Math.sin((double)(degrees * (float) (Math.PI / 180.0)));
   }

   public static float atan2Deg(float y, float x) {
      return (float)Math.atan2((double)y, (double)x) * (180.0F / (float)Math.PI);
   }

   public static float atan2(float y, float x) {
      return (float)Math.atan2((double)y, (double)x);
   }

   public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
      if (src == null) {
         throw new IllegalArgumentException("src cannot be null.");
      } else if (dest == null) {
         throw new IllegalArgumentException("dest cannot be null.");
      } else {
         try {
            System.arraycopy(src, srcPos, dest, destPos, length);
         } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException(
               "Src: " + Array.getLength(src) + ", " + srcPos + ", dest: " + Array.getLength(dest) + ", " + destPos + ", count: " + length
            );
         }
      }
   }

}
