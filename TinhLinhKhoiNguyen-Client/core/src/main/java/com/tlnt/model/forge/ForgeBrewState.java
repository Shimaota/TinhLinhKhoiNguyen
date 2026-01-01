package com.tlnt.model.forge;

import com.tlnt.model.item.Item;

public final class ForgeBrewState {
   public Item materialItem;
   public Item mainItem;
   public Item resultItem;
   public String infoText = "";
   public byte resultStatus = (byte)-1;

   public final void reset() {
      this.resultStatus = (byte)-1;
      if (this.materialItem != null) {
         this.materialItem.clear();
         this.materialItem = null;
      }

      if (this.mainItem != null) {
         this.mainItem.clear();
         this.mainItem = null;
      }

      if (this.resultItem != null) {
         this.resultItem.clear();
         this.resultItem = null;
      }

      this.infoText = "";
   }
}
