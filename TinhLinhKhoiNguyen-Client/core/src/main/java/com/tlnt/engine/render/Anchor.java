package com.tlnt.engine.render;

public enum Anchor {
   TOP_LEFT(1, 8),
   TOP_CENTER(2, 1),
   TOP_RIGHT(3, 16),
   MIDDLE_LEFT(4, 8),
   CENTER(5, 1),
   MIDDLE_RIGHT(6, 16),
   BOTTOM_LEFT(7, 8),
   BOTTOM_CENTER(8, 1),
   BOTTOM_RIGHT(9, 16);

   public int id;
   public int halign;

   public static Anchor[] getValues() {
      return (Anchor[])Anchor.values().clone();
   }

   Anchor(int id, int halign) {
      this.id = id;
      this.halign = halign;
   }

   public static Anchor fromId(int id) {
      Anchor[] values;
      int count = (values = getValues()).length;

      for (int i = 0; i < count; i++) {
         Anchor anchor;
         if ((anchor = values[i]).id == id) {
            return anchor;
         }
      }

      return null;
   }

}
