package com.tlnt.model.forge;

import com.tlnt.model.item.Item;

public final class ForgeUpgradeState {
   public Item materialItem1;
   public Item materialItem2;
   public Item equipmentItem;
   public Item resultItem;
   public String infoText = "";
   public byte resultStatus = (byte)-1;

   public final void reset() {
      this.resultStatus = (byte)-1;
      if (this.materialItem1 != null) {
         this.materialItem1.clear();
         this.materialItem1 = null;
      }

      if (this.materialItem2 != null) {
         this.materialItem2.clear();
         this.materialItem2 = null;
      }

      if (this.equipmentItem != null) {
         this.equipmentItem.clear();
         this.equipmentItem = null;
      }

      if (this.resultItem != null) {
         this.resultItem.clear();
         this.resultItem = null;
      }

      this.infoText = "";
   }
}
