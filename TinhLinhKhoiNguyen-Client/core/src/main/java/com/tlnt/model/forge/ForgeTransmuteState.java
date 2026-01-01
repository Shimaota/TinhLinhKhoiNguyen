package com.tlnt.model.forge;

import com.tlnt.model.item.Item;

/** "Transmute" tab (forge mode 6, decompiled C0110): slot 0 item to transmute, 1 new item, 2 transmute charm. */
public final class ForgeTransmuteState {
   public Item sourceItem;
   public Item targetItem;
   public Item charmItem;
   public String infoText = "";
   public byte resultStatus = (byte)-1;

   public final void reset() {
      this.resultStatus = (byte)-1;
      if (this.sourceItem != null) {
         this.sourceItem.clear();
         this.sourceItem = null;
      }

      if (this.targetItem != null) {
         this.targetItem.clear();
         this.targetItem = null;
      }

      if (this.charmItem != null) {
         this.charmItem.clear();
         this.charmItem = null;
      }

      this.infoText = "";
   }
}
