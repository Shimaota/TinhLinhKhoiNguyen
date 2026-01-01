package com.tlnt.model.worldmap;

public final class RegionInfo {
   public int regionId;
   public byte playerCount;
   public byte maxPlayers;
   public byte partyCount;
   public byte loadLevel;

   public RegionInfo(int regionId, byte playerCount, byte maxPlayers, byte partyCount, byte loadLevel) {
      this.regionId = regionId;
      this.playerCount = playerCount;
      this.maxPlayers = maxPlayers;
      this.partyCount = partyCount;
      this.loadLevel = loadLevel;
   }
}
