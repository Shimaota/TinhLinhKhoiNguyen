package com.tlnt.model.skill;

import com.badlogic.gdx.utils.Array;

public final class LearnedSkill {
   public SkillData skill;
   public SkillLevelData levelData;
   public long lastCastTime;

   public LearnedSkill(int skillId, int level, long lastCastTime) {
      this.skill = SkillData.getById(skillId);
      level = level;
      SkillData skillData = this.skill;
      Array.ArrayIterator it = this.skill.levels.iterator();

      SkillLevelData matchedLevel;
      while (true) {
         if (it.hasNext()) {
            SkillLevelData levelEntry;
            if ((levelEntry = (SkillLevelData)it.next()).level != level) {
               continue;
            }

            matchedLevel = levelEntry;
            break;
         }

         matchedLevel = null;
         break;
      }

      this.levelData = matchedLevel;
      this.lastCastTime = lastCastTime;
   }
}
