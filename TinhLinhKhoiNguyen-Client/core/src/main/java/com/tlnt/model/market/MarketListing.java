package com.tlnt.model.market;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.item.Item;

public final class MarketListing {
   public static final Pool<MarketListing> POOL = new MarketListingPool();
   public int listingId;
   public int sellerId;
   public String sellerName;
   public int price;
   public Item item;
   public long expireTime;
}
