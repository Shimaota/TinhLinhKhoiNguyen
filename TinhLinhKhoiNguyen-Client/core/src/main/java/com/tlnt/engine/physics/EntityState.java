package com.tlnt.engine.physics;

public final class EntityState {
   public static final int NEW = 1;
   public static final int PENDING_ADD;
   public static final int ADDED;
   public static final int PENDING_REMOVE = 4;
   public static final int REMOVED;

   static {
      PENDING_ADD = 2;
      ADDED = 3;
      REMOVED = 5;
      int[] values = new int[5];
      values[0] = NEW;
      values[1] = PENDING_ADD;
      values[2] = ADDED;
      values[3] = PENDING_REMOVE;
      values[4] = REMOVED;
   }
}
