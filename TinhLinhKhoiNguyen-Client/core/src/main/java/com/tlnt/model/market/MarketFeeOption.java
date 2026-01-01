package com.tlnt.model.market;

import com.badlogic.gdx.utils.Array;

public final class MarketFeeOption {
   public static final Array<MarketFeeOption> OPTIONS = new Array<>();
   public int id;
   public String name;
   public int feePercent;

   public static void clearAll() {
      OPTIONS.clear();
   }

   public static void register(MarketFeeOption option) {
      OPTIONS.add(option);
   }

   public MarketFeeOption(int id, String name, int feePercent) {
      this.id = id;
      this.name = name;
      this.feePercent = feePercent;
   }
}
