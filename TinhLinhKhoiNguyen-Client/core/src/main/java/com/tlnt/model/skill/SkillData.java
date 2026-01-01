package com.tlnt.model.skill;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public final class SkillData {
   public static ObjectMap<Integer, SkillData> registry = new ObjectMap<>();
   public int id;
   public String name;
   public int targetType;
   public int unlockLevel;
   public byte manaCostType;
   public String description;
   public String[] iconPaths;
   public Array<SkillLevelData> levels;

   public static SkillData getById(int id) {
      return registry.get(id);
   }

   public static void register(SkillData skill) {
      registry.put(skill.id, skill);
   }

   public static void clearAll() {
      registry.clear();
   }

   public SkillData(int id, String name, int targetType, int unlockLevel, byte manaCostType, String description, String[] iconPaths) {
      this.id = id;
      this.name = name;
      this.targetType = targetType;
      this.unlockLevel = unlockLevel;
      this.manaCostType = manaCostType;
      this.description = description;
      this.iconPaths = iconPaths;
      this.levels = new Array<>();
   }
}
