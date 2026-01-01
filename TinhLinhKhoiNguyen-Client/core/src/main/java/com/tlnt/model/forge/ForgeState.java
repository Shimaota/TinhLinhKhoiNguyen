package com.tlnt.model.forge;

public final class ForgeState {
   public int activeMode;
   public ForgeUpgradeState upgradeState;
   public ForgeSoulState soulState;
   public ForgeElementState elementState;
   public ForgeBrewState brewState;
   public ForgeCraftState craftState;
   public ForgeRefineState refineState;
   public ForgeTransmuteState transmuteState;

   public ForgeState() {
      this.activeMode = -1;
      this.upgradeState = new ForgeUpgradeState();
      this.soulState = new ForgeSoulState();
      this.elementState = new ForgeElementState();
      this.brewState = new ForgeBrewState();
      this.craftState = new ForgeCraftState();
      this.refineState = new ForgeRefineState();
      this.transmuteState = new ForgeTransmuteState();
   }

   public final void reset() {
      this.activeMode = -1;
      this.upgradeState.reset();
      this.soulState.reset();
      this.elementState.reset();
      this.brewState.reset();
      this.craftState.reset();
      this.refineState.reset();
      this.transmuteState.reset();
   }
}
