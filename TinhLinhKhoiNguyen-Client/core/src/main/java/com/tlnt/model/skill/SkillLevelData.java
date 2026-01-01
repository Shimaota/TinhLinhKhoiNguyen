package com.tlnt.model.skill;

public final class SkillLevelData {
   public SkillData skill;
   public int level;
   public int rangeX;
   public int rangeY;
   public int manaCost;
   public int cooldownMillis;
   public int requiredLevel;
   public int upgradeCost;
   /** Gold cost to upgrade to this level (decompiled C0169.f1351). */
   public long goldCost;
   /** Experience cost to upgrade to this level (decompiled C0169.f1341). */
   public long expCost;
   public String effectDescription;
   public SkillLevelData prevLevel;
   public SkillLevelData nextLevel;

   public SkillLevelData(SkillData skill, int level, int rangeX, int rangeY, int manaCost, int cooldownMillis, int requiredLevel, int upgradeCost, long goldCost, long expCost, String effectDescription) {
      this.skill = skill;
      this.level = level;
      this.rangeX = rangeX;
      this.rangeY = rangeY;
      this.manaCost = manaCost;
      this.cooldownMillis = cooldownMillis;
      this.requiredLevel = requiredLevel;
      this.upgradeCost = upgradeCost;
      this.goldCost = goldCost;
      this.expCost = expCost;
      this.effectDescription = effectDescription;
   }

   public final float getRange() {
      return (float)(Math.sqrt((double)(this.rangeX * this.rangeX + this.rangeY + this.rangeY)) / 100.0);
   }
}
