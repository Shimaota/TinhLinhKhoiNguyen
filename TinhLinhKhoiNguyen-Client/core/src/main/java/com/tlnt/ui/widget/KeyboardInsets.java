package com.tlnt.ui.widget;

/**
 * Height of the visible soft keyboard as a 0..1 fraction of the screen height (independent of pixels / points).
 * Written by the Android launcher (KeyboardHeightProvider) and iOS (UIKeyboard notifications); read by Scene2dScreen.render to shift
 * the UI camera when the focused field is covered by the keyboard. PC never writes it -> always 0.
 */
public final class KeyboardInsets {
   private static volatile float heightFraction;
   private static volatile int version;

   private KeyboardInsets() {
   }

   /** Called from the platform UI thread (single writer thread). */
   public static void setHeightFraction(float fraction) {
      if (!(fraction > 0.0F)) {
         fraction = 0.0F;
      } else if (fraction > 0.9F) {
         fraction = 0.9F;
      }

      if (fraction != heightFraction) {
         heightFraction = fraction;
         version++;
      }
   }

   public static float getHeightFraction() {
      return heightFraction;
   }

   public static int getVersion() {
      return version;
   }
}
