package com.tlnt.model.consignment;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.ItemTemplate;

public final class ConsignmentItem {
   public static Pool<ConsignmentItem> pool = new ConsignmentItemPool();
   public ConsignmentCategory category;
   public int slot;
   public Item itemData;
   public ItemTemplate itemConfig;
   public int quantity;
}
