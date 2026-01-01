package com.tlnt.model.skill;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.tlnt.util.GameUtils;

public final class TalentData {
   public static final ObjectMap<Integer, TalentData> REGISTRY = new OrderedMap<>();
   public int id;
   public String name;
   public String iconPath;
   public String description;
   public int pointCost;

   public static void register(TalentData talent) {
      REGISTRY.put(talent.id, talent);
   }

   public static void clearAll() {
      REGISTRY.clear();
   }

   public static Array<TalentData> getAll() {
      return GameUtils.mapValuesToArray(REGISTRY);
   }

   public TalentData(int id, String name, String iconPath, String description, int pointCost) {
      this.id = id;
      this.name = name;
      this.iconPath = iconPath;
      this.description = description;
      this.pointCost = pointCost;
   }
}
