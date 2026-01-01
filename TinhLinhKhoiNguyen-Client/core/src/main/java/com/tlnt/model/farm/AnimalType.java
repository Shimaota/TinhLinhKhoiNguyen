package com.tlnt.model.farm;

import com.badlogic.gdx.utils.ObjectMap;

public final class AnimalType {
   public static ObjectMap<Integer, AnimalType> typesById = new ObjectMap<>();
   public int id;
   public String growingName;
   public String matureName;
   public String skeletonPath;
   public String skinName;
   public float scale;
   public int growDurationMs;

   public static void register(AnimalType animalType) {
      typesById.put(animalType.id, animalType);
   }

   public static void clearAll() {
      typesById.clear();
   }

   public static AnimalType getById(int id) {
      return typesById.get(id);
   }

   public AnimalType(int id, String growingName, String matureName, String skeletonPath, String skinName, float scale, int growDurationMs) {
      this.id = id;
      this.growingName = growingName;
      this.matureName = matureName;
      this.skeletonPath = skeletonPath;
      this.skinName = skinName;
      this.scale = scale;
      this.growDurationMs = growDurationMs;
   }
}
