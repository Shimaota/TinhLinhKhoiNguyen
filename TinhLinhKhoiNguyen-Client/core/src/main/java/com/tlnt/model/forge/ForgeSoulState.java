package com.tlnt.model.forge;

import com.tlnt.model.item.Item;

public final class ForgeSoulState {
   public int soulLevel;
   public Item equipmentItem;
   public Item materialItem2;
   public Item materialItem1;
   public Item socketItem1;
   public Item socketItem2;
   public Item socketItem3;
   public Item socketItem4;
   public Item socketItem5;
   public String infoText = "";
   public byte resultStatus;

   public ForgeSoulState() {
      this.resultStatus = (byte)-1;
   }

   public final void reset() {
      this.resultStatus = (byte)-1;
      this.soulLevel = 0;
      if (this.equipmentItem != null) {
         this.equipmentItem.clear();
         this.equipmentItem = null;
      }

      if (this.materialItem1 != null) {
         this.materialItem1.clear();
         this.materialItem1 = null;
      }

      if (this.materialItem2 != null) {
         this.materialItem2.clear();
         this.materialItem2 = null;
      }

      this.infoText = "";
   }
}
