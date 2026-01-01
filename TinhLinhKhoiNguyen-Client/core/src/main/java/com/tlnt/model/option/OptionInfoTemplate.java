package com.tlnt.model.option;

import com.badlogic.gdx.utils.ObjectMap;

public final class OptionInfoTemplate {
   public static ObjectMap<Integer, OptionInfoTemplate> templates = new ObjectMap<>();
   public int id;
   public String name;
   public String icon;
   public String description;

   public static void register(OptionInfoTemplate template) {
      templates.put(template.id, template);
   }

   public static OptionInfoTemplate getById(int id) {
      return templates.get(id);
   }

   public OptionInfoTemplate(int id, String name, String icon, String description) {
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.description = description;
   }
}
