package com.tlnt.model.skill;

import com.badlogic.gdx.utils.Array;

public final class TalentPointCap {
   public static final Array<TalentPointCap> ENTRIES = new Array<>();
   public int id;
   public int requiredLevel;
   public int[] talentPointCaps;

   public static void clearAll() {
      ENTRIES.clear();
   }

   public static void register(TalentPointCap cap) {
      ENTRIES.add(cap);
   }

   public static TalentPointCap findNextCap(int points, int talentId) {
      for (TalentPointCap cap : ENTRIES) {
         if (points < cap.talentPointCaps[talentId - 1]) {
            return cap;
         }
      }

      return null;
   }

   public TalentPointCap(int id, int requiredLevel, int[] talentPointCaps) {
      this.id = id;
      this.requiredLevel = requiredLevel;
      this.talentPointCaps = talentPointCaps;
   }
}
