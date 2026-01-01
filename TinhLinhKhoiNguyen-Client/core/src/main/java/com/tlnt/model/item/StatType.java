package com.tlnt.model.item;

import com.badlogic.gdx.utils.ObjectMap;

public final class StatType {
   public static ObjectMap<Integer, StatType> statTypesById = new ObjectMap<>();
   public int id;
   public String formatPattern;

   public static void register(StatType statType) {
      statTypesById.put(statType.id, statType);
   }

   public static StatType getById(int id) {
      return statTypesById.get(id);
   }

   public StatType(int id, String formatPattern) {
      this.id = id;
      this.formatPattern = formatPattern;
   }
}
