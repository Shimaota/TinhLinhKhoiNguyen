package com.tlnt.model.quest.daily;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.item.Item;

public final class DailyMilestone {
   public static Pool<DailyMilestone> POOL = new DailyMilestonePool();
   public int milestoneId;
   public int requiredPoints;
   public Item rewardItem;
   public boolean claimed;
}
