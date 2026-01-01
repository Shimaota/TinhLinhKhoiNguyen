package com.tlnt.thirdparty.spine;

public final class EventData {
   public final String name;
   public int intValue;
   public float floatValue;
   public String stringValue;
   public String audioPath;
   public float volume;
   public float balance;

   public EventData(String name) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else {
         this.name = name;
      }
   }

   @Override
   public final String toString() {
      return this.name;
   }

}
