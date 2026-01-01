package com.tlnt.model.quest.daily;

import com.badlogic.gdx.utils.Array;

public final class DailyActivityState {
   public Array<DailyMilestone> milestones = new Array<>();
   public int currentDiligencePoints;
   public long remainingTime;
   public Array<DailyQuestTask> questTasks = new Array<>();
   public boolean hasNewlyCompletedTasks;
}
