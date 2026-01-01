package com.tlnt.model.quest.daily;

import com.badlogic.gdx.utils.Pool;

public final class DailyMilestonePool extends Pool<DailyMilestone> {

   @Override
   public DailyMilestone newObject() {
      return new com.tlnt.model.quest.daily.DailyMilestone();
   }
}
