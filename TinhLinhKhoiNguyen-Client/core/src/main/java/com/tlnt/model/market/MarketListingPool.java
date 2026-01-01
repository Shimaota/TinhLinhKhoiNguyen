package com.tlnt.model.market;

import com.badlogic.gdx.utils.Pool;

public final class MarketListingPool extends Pool<MarketListing> {

   @Override
   public MarketListing newObject() {
      return new com.tlnt.model.market.MarketListing();
   }
}
