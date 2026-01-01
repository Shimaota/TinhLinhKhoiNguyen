package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.tlnt.thirdparty.spine.Slot;

public final class Sequence {
   public static int nextId;
   public final int id = nextId();
   public final TextureRegion[] regions;
   public int start;
   public int digits;
   public int setupIndex;

   public Sequence(int count) {
      this.regions = new TextureRegion[count];
   }

   public final void apply(Slot slot, HasTextureRegion attachment) {
      int index;
      if ((index = slot.getSequenceIndex()) == -1) {
         index = this.setupIndex;
      }

      if (index >= this.regions.length) {
         int regionCount = this.regions.length;
         index = regionCount - 1;
      }

      TextureRegion region = this.regions[index];
      if (attachment.getRegion() != region) {
         attachment.setRegion(region);
         attachment.updateRegion();
      }
   }

   public final String getPath(String basePath, int index) {
      StringBuilder buffer;
      (buffer = new StringBuilder(basePath.length() + this.digits)).append(basePath);
      basePath = Integer.toString(this.start + index);

      for (int i = this.digits - basePath.length(); i > 0; i--) {
         buffer.append((char)48);
      }

      buffer.append(basePath);
      return buffer.toString();
   }

   public final void setStart(int start) {
      this.start = start;
   }

   public final void setDigits(int digits) {
      this.digits = digits;
   }

   public final void setSetupIndex(int setupIndex) {
      this.setupIndex = setupIndex;
   }

   public final TextureRegion[] getRegions() {
      return this.regions;
   }

   public final int getId() {
      return this.id;
   }

   public static synchronized int nextId() {
      int id = nextId;
      nextId += 1;
      return id;
   }

   public static final class SequenceMode {
      public static final int HOLD = 1;
      public static final int ONCE = 2;
      public static final int LOOP = 3;
      public static final int PINGPONG = 4;
      public static final int ONCE_REVERSE = 5;
      public static final int LOOP_REVERSE = 6;
      public static final int PINGPONG_REVERSE = 7;
      private static final int[] $VALUES;
      public static final int[] VALUES;

      public static int[] values() {
         return (int[])$VALUES.clone();
      }

      static {
         int[] modes = new int[7];
         modes[0] = HOLD;
         modes[1] = ONCE;
         modes[2] = LOOP;
         modes[3] = PINGPONG;
         modes[4] = ONCE_REVERSE;
         modes[5] = LOOP_REVERSE;
         modes[6] = PINGPONG_REVERSE;
         $VALUES = modes;
         VALUES = values();
      }
   }
}
