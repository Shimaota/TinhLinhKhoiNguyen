package com.tlnt.model.forge;

import com.tlnt.model.item.Item;

public final class ForgeRefineState {
   public Item inputItem;
   public Item resultItem;
   public int refineOption;
   public String infoText = "";
   public byte resultStatus;

   public ForgeRefineState() {
      this.resultStatus = (byte)-1;
   }

   public final void reset() {
      this.resultStatus = (byte)-1;
      if (this.inputItem != null) {
         this.inputItem.clear();
         this.inputItem = null;
      }

      if (this.resultItem != null) {
         this.resultItem.clear();
         this.resultItem = null;
      }

      this.refineOption = 0;
      this.infoText = "";
   }
}
