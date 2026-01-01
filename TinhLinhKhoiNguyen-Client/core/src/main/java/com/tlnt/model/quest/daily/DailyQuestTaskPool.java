package com.tlnt.model.quest.daily;

import com.badlogic.gdx.utils.Pool;

public final class DailyQuestTaskPool extends Pool<DailyQuestTask> {

   @Override
   public DailyQuestTask newObject() {
      return new com.tlnt.model.quest.daily.DailyQuestTask();
   }
}
