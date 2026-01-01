package com.tlnt.engine.physics.component;

public final class ComponentState {
   public static final int CREATED;
   public static final int PENDING_ADD;
   public static final int ADDED;
   public static final int ACTIVE = 4;
   public static final int PENDING_REMOVE;
   public static final int REMOVED = 6;

   static {
      CREATED = 1;
      PENDING_ADD = 2;
      ADDED = 3;
      PENDING_REMOVE = 5;
      int[] values = new int[6];
      values[0] = CREATED;
      values[1] = PENDING_ADD;
      values[2] = ADDED;
      values[3] = ACTIVE;
      values[4] = PENDING_REMOVE;
      values[5] = REMOVED;
   }
}
