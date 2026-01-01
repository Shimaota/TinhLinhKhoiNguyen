package com.tlnt.model.giftcode;

public final class GiftcodeEvent {
   public int id;
   public String title;
   public String iconPath;
   public GiftcodeEventMilestone[] milestones;
   public boolean hasClaimableReward;

   public GiftcodeEvent(int id, String title, String iconPath) {
      this.id = id;
      this.title = title;
      this.iconPath = iconPath;
   }
}
