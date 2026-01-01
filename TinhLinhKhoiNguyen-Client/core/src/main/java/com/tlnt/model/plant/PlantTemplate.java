package com.tlnt.model.plant;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;

public final class PlantTemplate {
   public static ObjectMap<Integer, PlantTemplate> templatesById = new OrderedMap<>();
   public int id;
   public String name;
   public float offsetX;
   public float offsetY;
   public String skeletonName;
   public String idleAnimation;
   public float skeletonScale;

   public static void clearTemplates() {
      templatesById.clear();
   }

   public static void registerTemplate(PlantTemplate template) {
      templatesById.put(template.id, template);
   }

   public static PlantTemplate getTemplateById(int id) {
      return templatesById.get(id);
   }

   public PlantTemplate(int id, String name, float offsetX, float offsetY, String skeletonName, String idleAnimation, float skeletonScale) {
      this.id = id;
      this.name = name;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.skeletonName = skeletonName;
      this.idleAnimation = idleAnimation;
      this.skeletonScale = skeletonScale;
   }
}
