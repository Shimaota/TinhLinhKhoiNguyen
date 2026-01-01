package com.tlnt.model.item;

import com.badlogic.gdx.utils.ObjectMap;
import com.tlnt.model.character.CharacterTemplate;

public final class ItemTemplate {
   public static ObjectMap<Integer, ItemTemplate> templatesById = new ObjectMap<>();
   public int id;
   public ItemType type;
   public CharacterTemplate requiredClass;
   public String name;
   public String description;
   public String iconPath;
   public String previewImagePath;
   public int requiredLevel;
   public String emblemPath;

   public static ItemTemplate getById(int id) {
      return templatesById.get(id);
   }

   public static void register(ItemTemplate template) {
      templatesById.put(template.id, template);
   }

   public ItemTemplate(int id, int typeId, int classId, String name, String description, String iconPath, String previewImagePath, int requiredLevel, String emblemPath) {
      this.id = id;
      this.type = ItemType.getById(typeId);
      this.requiredClass = CharacterTemplate.getById(classId);
      this.name = name;
      this.description = description;
      this.iconPath = iconPath;
      this.previewImagePath = previewImagePath;
      this.requiredLevel = requiredLevel;
      this.emblemPath = emblemPath;
   }
}
