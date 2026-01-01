package com.tlnt.model.giftcode;

import com.badlogic.gdx.utils.Array;

public final class GiftcodeState {
   public Array<GiftcodeEvent> events = new Array<>();
   public boolean hasClaimableReward;
   public String giftcodeListText = "";

   public final void updateClaimableFlags() {
      int anyClaimable = 0;
      Array.ArrayIterator it = this.events.iterator();

      while (it.hasNext()) {
         GiftcodeEvent event;
         GiftcodeEvent evt;
         GiftcodeEventMilestone[] milestones;
         int milestoneCount = (milestones = (evt = event = (GiftcodeEvent)it.next()).milestones).length;
         int i = 0;

         while (true) {
            if (i >= milestoneCount) {
               evt.hasClaimableReward = false;
               break;
            }

            GiftcodeEventMilestone milestone;
            if ((milestone = milestones[i]).claimStatus == 1) {
               evt.hasClaimableReward = true;
               break;
            }

            i++;
         }

         if (event.hasClaimableReward) {
            anyClaimable = 1;
         }
      }

      this.hasClaimableReward = (anyClaimable != 0);
   }
}
