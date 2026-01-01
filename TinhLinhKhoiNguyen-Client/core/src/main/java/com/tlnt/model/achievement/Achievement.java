package com.tlnt.model.achievement;

import com.tlnt.model.item.Item;

public final class Achievement {
   public int id;
   public String title;
   public String description;
   public int targetCount;
   public Item[] rewardItems;
   public int progress;
   public byte status;

   public Achievement(int id, String title, String description, int targetCount, Item[] rewardItems, int progress, byte status) {
      this.id = id;
      this.title = title;
      this.description = description;
      this.targetCount = targetCount;
      this.rewardItems = rewardItems;
      this.progress = progress;
      this.status = status;
   }
}
