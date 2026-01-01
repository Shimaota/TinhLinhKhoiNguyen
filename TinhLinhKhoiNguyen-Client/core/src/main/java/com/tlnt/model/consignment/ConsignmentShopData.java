package com.tlnt.model.consignment;

import com.badlogic.gdx.utils.Array;

public final class ConsignmentShopData {
   public int shopId;
   public String shopName;
   public byte shopType;
   public Array<ConsignmentCategory> categories = new Array<>();
   public boolean dirty;

   public final void clearCategories() {
      Array.ArrayIterator it = this.categories.iterator();

      while (it.hasNext()) {
         ((ConsignmentCategory)it.next()).freeToPool();
      }

      this.categories.clear();
   }
}
