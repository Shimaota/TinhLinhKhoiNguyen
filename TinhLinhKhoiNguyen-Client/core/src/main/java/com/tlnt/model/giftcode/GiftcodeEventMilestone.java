package com.tlnt.model.giftcode;

import com.tlnt.model.item.Item;

public final class GiftcodeEventMilestone {
   public GiftcodeEvent event;
   public int id;
   public String description;
   public byte claimStatus;
   public Item[] rewardItems;

   public GiftcodeEventMilestone(GiftcodeEvent event, int id, String description, byte claimStatus) {
      this.event = event;
      this.id = id;
      this.description = description;
      this.claimStatus = claimStatus;
   }
}
