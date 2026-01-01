package com.tlnt.model.social;

import com.badlogic.gdx.utils.ObjectMap;

public final class TribeSkill {
   public static final ObjectMap<Integer, TribeSkill> REGISTRY = new ObjectMap<>();
   public int id;
   public String name;
   public String iconPath;
   public String description;

   public static void clearAll() {
      REGISTRY.clear();
   }

   public static void register(TribeSkill skill) {
      REGISTRY.put(skill.id, skill);
   }

   public static TribeSkill getById(int id) {
      return REGISTRY.get(id);
   }

   public TribeSkill(int id, String name, String iconPath, String description) {
      this.id = id;
      this.name = name;
      this.iconPath = iconPath;
      this.description = description;
   }
}
