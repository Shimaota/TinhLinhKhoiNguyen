package com.tlnt.model.player;

import com.tlnt.model.character.CharacterClass;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.model.title.TitleData;

public final class PlayerStats {
   public FactionStatus factionStatus = new FactionStatus();
   public boolean isEnemy;
   public boolean isInParty;
   public int partyLeaderId;
   public boolean isAlive;
   public byte classId;
   public String characterName;
   public int partyId;
   public int guildId;
   public String guildName;
   public String guildBadgeTexture;
   public String avatarTexture;
   public CharacterClass classData;
   public CharacterTemplate jobData;
   public boolean isTransparent;
   public String weaponTexture;
   public byte weaponType;
   public boolean showHealthBar;
   public int movementMode;
   public String skeletonKey;
   public String[] equipmentSkinKeys;
   public TitleData titleData;
   public String titleKey;
   public int level;
   public long currentExp;
   public long maxExp;
   public int[] talentPointsArray;
   public int availableTalentPoints;
   public int activityPointsCurrent;
   public int activityPointsMax;
   public int activityPoints;
   public long currentHp;
   public long maxHp;
   public long currentMp;
   public long maxMp;
   public long attackPower;
   public long attackVsMob;
   public long critChance;
   public long critDamagePercent;
   public long armor;
   public long armorPen;
   public long evasion;
   public long accuracy;
   public long moveSpeed;
   public long hpRegen;
   public long mpRegen;
   public long hpLeechPercent;
   public long mpLeechPercent;
   public long hpLeechVsMobPercent;
   public long mpLeechVsMobPercent;
   public long reflectDamagePercent;
   public long stunDamage;
   public long weakenDamage;
   public long iceDamage;
   public long burnDamage;
   public long thunderDamage;
   public long critResistPercent;
   public long damageAmpPercent;
   public long damageAbsorbPercent;
   public long effectResistPercent;
   public long damageResist;
   public long goldBonusPercent;
   public long expBonusPercent;

   public PlayerStats() {
      this.weaponTexture = "lv-1";
      this.weaponType = (byte)-1;
      this.movementMode = MoveSpeedTable.SPEED_NORMAL;
   }

   public final float getHpRatio() {
      return (float)this.currentHp / (float)this.maxHp;
   }

   public final float getMpRatio() {
      return (float)this.currentMp / (float)this.maxMp;
   }

   public final float getActivityPointsRatio() {
      return this.activityPointsMax == 0 ? 0.0F : (float)this.activityPointsCurrent / (float)this.activityPointsMax;
   }

   public final float getMoveSpeedFactor() {
      return this.moveSpeed > 0 ? (float)this.moveSpeed / 100.0F : 4.0F;
   }

}
