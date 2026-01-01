package com.tlnt.model.social;

import com.badlogic.gdx.utils.ObjectMap;

public final class TribeEmblem {
   public static final ObjectMap<Integer, TribeEmblem> REGISTRY = new ObjectMap<>();
   public int id;
   public String name;
   public String[] framePaths;
   public int frameRate;

   public static void clearAll() {
      REGISTRY.clear();
   }

   public static void register(TribeEmblem emblem) {
      REGISTRY.put(emblem.id, emblem);
   }

   public static TribeEmblem getById(int id) {
      return REGISTRY.get(id);
   }

   public TribeEmblem(int id, String name, String[] framePaths, int frameRate) {
      this.id = id;
      this.name = name;
      this.framePaths = framePaths;
      this.frameRate = frameRate;
   }
}
