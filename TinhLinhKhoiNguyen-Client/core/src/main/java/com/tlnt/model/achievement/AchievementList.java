package com.tlnt.model.achievement;

import com.badlogic.gdx.utils.Array;

public final class AchievementList {
   public Array<Achievement> achievements = new Array<>();
   public boolean hasClaimableReward;

   public final void updateClaimableFlag() {
      Array.ArrayIterator it = this.achievements.iterator();

      while (it.hasNext()) {
         if (((Achievement)it.next()).status == 1) {
            this.hasClaimableReward = true;
            return;
         }
      }

      this.hasClaimableReward = false;
   }
}
