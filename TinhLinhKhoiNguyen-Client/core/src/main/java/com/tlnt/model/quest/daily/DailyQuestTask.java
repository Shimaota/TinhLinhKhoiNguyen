package com.tlnt.model.quest.daily;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.item.Item;

public final class DailyQuestTask {
   public static final Pool<DailyQuestTask> POOL = new DailyQuestTaskPool();
   public int questIndex;
   public String questName;
   public String questDescription;
   public int targetProgress;
   public int currentProgress;
   public Array<Item> rewards = new Array<>();
   public boolean rewardClaimed;
}
