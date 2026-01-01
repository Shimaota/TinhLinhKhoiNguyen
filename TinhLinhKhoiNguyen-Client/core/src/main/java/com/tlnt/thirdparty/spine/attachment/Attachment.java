package com.tlnt.thirdparty.spine.attachment;

public abstract class Attachment {
   public String bones;

   public Attachment(String name) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else {
         this.bones = name;
      }
   }

   @Override
   public String toString() {
      return this.bones;
   }

}
