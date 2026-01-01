package com.tlnt.thirdparty.spine.attachment;

public final class AttachmentType {
   public static final int REGION = 1;
   public static final int BOUNDINGBOX = 2;
   public static final int MESH = 3;
   public static final int LINKEDMESH = 4;
   public static final int PATH = 5;
   public static final int POINT = 6;
   public static final int CLIPPING = 7;
   public static int SKELETON = 8;
   public static final int[] VALUES;
   public static final int[] $VALUES;

   public static int[] values() {
      return (int[])$VALUES.clone();
   }

   static {
      int[] types = new int[8];
      types[0] = REGION;
      types[1] = BOUNDINGBOX;
      types[2] = MESH;
      types[3] = LINKEDMESH;
      types[4] = PATH;
      types[5] = POINT;
      types[6] = CLIPPING;
      types[7] = SKELETON;
      $VALUES = types;
      VALUES = values();
   }
}
