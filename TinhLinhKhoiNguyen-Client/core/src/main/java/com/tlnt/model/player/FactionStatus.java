package com.tlnt.model.player;

public final class FactionStatus {
   public FactionStatus.FactionType faction = FactionStatus.FactionType.NEUTRAL;

   public static enum FactionType {
      NEUTRAL("ui/32"),
      FACTION_A("ui/33"),
      FACTION_B("ui/34"),
      FACTION_C("ui/166");

      public String displayName;

      FactionType(String displayName) {
         this.displayName = displayName;
      }

   }
}
