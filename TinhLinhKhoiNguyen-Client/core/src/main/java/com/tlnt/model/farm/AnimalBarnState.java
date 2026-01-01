package com.tlnt.model.farm;

public final class AnimalBarnState {
   public int level;
   public int maxHarvestCount;
   public long productionStartTime;
   public int productionIntervalMs = 1;
   public boolean upgrading;
   public long upgradeStartTime;
   public long upgradeDurationMs;
}
