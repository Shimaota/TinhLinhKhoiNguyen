package com.tlnt.thirdparty.spine;

public abstract class ConstraintData {
   public final String name;
   public int order;
   public boolean skinRequired;

   public ConstraintData(String name) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else {
         this.name = name;
      }
   }

   @Override
   public String toString() {
      return this.name;
   }

}
