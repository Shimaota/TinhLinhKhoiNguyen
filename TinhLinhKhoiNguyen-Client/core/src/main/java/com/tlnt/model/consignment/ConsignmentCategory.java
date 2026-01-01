package com.tlnt.model.consignment;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public final class ConsignmentCategory {
   public static Pool<ConsignmentCategory> pool = new ConsignmentCategoryPool();
   public ConsignmentShopData shopData;
   public int index;
   public String tabName;
   public String tabNameAlt;
   public Array<ConsignmentItem> items = new Array<>();

   public final void freeToPool() {
      for (ConsignmentItem item : this.items) {
         ConsignmentItem.pool.free(item);
      }

      this.items.clear();
      pool.free(this);
   }
}
