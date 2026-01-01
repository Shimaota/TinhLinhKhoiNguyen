package com.tlnt.model.combopack;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.ItemTemplate;

public final class ComboPack {
   public static Pool<ComboPack> pool = new ComboPackPool();
   public ComboPackCatalog catalog;
   public int index;
   public String name;
   public Array<Item> items = new Array<>();
   public ItemTemplate priceItem;
   public int price;
   public boolean purchased;
   /** Client-side guard until the purchase result arrives (or the retry window expires). */
   public boolean purchasePending;
   public long purchaseRequestedAt;
}
