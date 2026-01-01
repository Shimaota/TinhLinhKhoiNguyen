package com.tlnt.thirdparty.spine;

public final class Event {
   public final EventData data;
   public int intValue;
   public float floatValue;
   public String stringValue;
   public float volume;
   public float balance;
   public final float time;

   public Event(float time, EventData data) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.time = time;
         this.data = data;
      }
   }

   public final EventData getData() {
      return this.data;
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

}
