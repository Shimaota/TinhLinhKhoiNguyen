package com.tlnt.model.ranking;

import com.badlogic.gdx.utils.Array;

public final class RankingCategory {
   public int categoryId;
   public String name;
   public String description;
   public Array<RankEntry> entries = new Array<>();

   public RankingCategory(int categoryId, String name, String description) {
      this.categoryId = categoryId;
      this.name = name;
      this.description = description;
   }
}
