package com.tlnt.model.character;

import com.badlogic.gdx.utils.ObjectMap;

public final class CharacterClass {
   public static final ObjectMap<Integer, CharacterClass> REGISTRY = new ObjectMap<>();
   public int id;
   public String name;

   public static void clearAll() {
      REGISTRY.clear();
   }

   public static void register(CharacterClass characterClass) {
      REGISTRY.put(characterClass.id, characterClass);
   }

   public static CharacterClass getById(int id) {
      return REGISTRY.get(id);
   }

   public CharacterClass(int id, String name) {
      this.id = id;
      this.name = name;
   }
}
