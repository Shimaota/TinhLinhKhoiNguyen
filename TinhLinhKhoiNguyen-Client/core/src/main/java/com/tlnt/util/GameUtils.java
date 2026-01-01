package com.tlnt.util;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.ObjectMap;
import java.text.DecimalFormat;
import java.util.Random;

public final class GameUtils {
   public static Random random = new Random();

   public static String formatCompactNumber(long value) {
      if (value < 10000000L) {
         String plainFormat = "%,d";
         Object[] plainArgs = new Object[1];
         plainArgs[0] = value;
         return String.format(plainFormat, plainArgs);
      } else {
         String[] suffixArray = new String[5];
         suffixArray[0] = "";
         suffixArray[1] = "k";
         suffixArray[2] = "m";
         suffixArray[3] = "b";
         suffixArray[4] = "t";
         String[] suffixes = suffixArray;
         int magnitude = (int)(Math.log10((double)value) / 3.0);
         int suffixCount = suffixes.length;
         int suffixIndex = Math.min(magnitude, 4);
         double scaled;
         if ((scaled = Math.floor((double)value / Math.pow(1000.0, (double)suffixIndex) * 10.0) / 10.0) == (double)((long)scaled)) {
            String intFormat = "%d%s";
            Object[] intArgs = new Object[2];
            intArgs[0] = (long)scaled;
            intArgs[1] = suffixes[suffixIndex];
            return String.format(intFormat, intArgs);
         } else {
            String decimalFormat = "%.1f%s";
            Object[] decimalArgs = new Object[2];
            decimalArgs[0] = scaled;
            decimalArgs[1] = suffixes[suffixIndex];
            return String.format(decimalFormat, decimalArgs);
         }
      }
   }

   public static String formatFileSize(long bytes) {
      if (bytes < 1024L) {
         return bytes + " B";
      } else if (bytes < 1048576L) {
         String kbFormat = "%.2f KB";
         Object[] kbArgs = new Object[1];
         kbArgs[0] = (double)bytes / 1024.0;
         return String.format(kbFormat, kbArgs);
      } else if (bytes < 1073741824L) {
         String mbFormat = "%.2f MB";
         Object[] mbArgs = new Object[1];
         mbArgs[0] = (double)bytes / 1048576.0;
         return String.format(mbFormat, mbArgs);
      } else {
         String gbFormat = "%.2f GB";
         Object[] gbArgs = new Object[1];
         gbArgs[0] = (double)bytes / 1.0737418E9F;
         return String.format(gbFormat, gbArgs);
      }
   }

   public static <K, V> Array<V> mapValuesToArray(ObjectMap<K, V> map) {
      Array result = new Array();

      for (Object value : map.values()) {
         result.add(value);
      }

      return result;
   }

   public static float randomFloat(float min, float max) {
      return min + random.nextFloat() * (max - min);
   }

   public static int randomInt(int min, int max) {
      if (min > max) {
         return min;
      } else {
         int range;
         return (range = max - min + 1) <= 0 ? min : min + random.nextInt(range);
      }
   }

   public static float randomFloat01() {
      return random.nextFloat();
   }

   public static boolean rollChance(int chance, int outOf) {
      if (random.nextInt(outOf) < chance) {
         return true;
      } else {
         return false;
      }
   }

   public static boolean hasElapsed(long startTime, int intervalMs) {
      if (System.currentTimeMillis() - startTime >= (long)intervalMs) {
         return true;
      } else {
         return false;
      }
   }

   public static Texture decodeTexture(byte[] data) {
      Pixmap pixmap;
      try {
         pixmap = new Pixmap(data, 0, data.length);
      } catch (Exception e) {
         int swapLength = Math.min(data.length, 51);
         data = (byte[])data.clone();

         for (int i = 0; i < swapLength / 2; i++) {
            byte tmp = data[i];
            data[i] = data[swapLength - 1 - i];
            data[swapLength - 1 - i] = tmp;
         }

         pixmap = new Pixmap(data, 0, data.length);
      }

      // new Texture(Pixmap) uploads the image to the GPU immediately and does not keep the Pixmap (the texture is not "managed") -> free it right away.
      // The original did NOT dispose: every downloaded image / atlas page leaked w×h×4 bytes of native RAM (a 2048² page = 16 MB) — RAM grew
      // on every map change (2026-10-19: 130 MB Java heap but a 2.4 GB process).
      try {
         return new Texture(pixmap);
      } finally {
         pixmap.dispose();
      }
   }

   public static void sleepMillis(int millis) {
      try {
         Thread.sleep((long)millis);
      } catch (Exception e) {
      }
   }

   public static float oscillate(float period, float min, float max) {
      float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
      max -= min;
      period = max * 2.0F / period;
      period = time * period % (max * 2.0F);
      return min + (max - Math.abs(period - max));
   }

   public static float oscillateInterpolated(Interpolation interpolation, float period, float min, float max) {
      float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
      max -= min;
      float cycle;
      period = (cycle = time / period % 2.0F) <= 1.0F ? cycle : 2.0F - cycle;
      float progress = interpolation.apply(period);
      return min + progress * max;
   }

   public static int timeFrameIndex(int frameCount, int fps) {
      if (frameCount > 0 && fps > 0) {
         long frameMillis = 1000L / (long)fps;
         return (int)(System.currentTimeMillis() / frameMillis % (long)frameCount);
      } else {
         return 0;
      }
   }

   static {
      new JsonReader();
      new DecimalFormat("#,###");
   }

}
