package com.tlnt.model.mob;

import com.badlogic.gdx.utils.ObjectMap;

public final class MobTemplate {
   public static ObjectMap<Integer, MobTemplate> templatesById = new ObjectMap<>();
   public int id;
   public String name;
   public int wanderType;
   public int wanderRange;
   public String skeletonPath;
   public float scale;
   public MobTemplate.MobVariant[] variants;

   public static MobTemplate getById(int id) {
      return templatesById.get(id);
   }

   public static void register(MobTemplate template) {
      templatesById.put(template.id, template);
   }

   public static void clearAll() {
      templatesById.clear();
   }

   public MobTemplate(int id, String name, int wanderType, int wanderRange, String skeletonPath, float scale, MobTemplate.MobVariant[] variants) {
      this.id = id;
      this.name = name;
      this.wanderType = wanderType;
      this.wanderRange = wanderRange;
      this.skeletonPath = skeletonPath;
      this.scale = scale;
      this.variants = variants;
   }

   public static class MobVariant {
      public String displayName;
      public String skinName;
      public float moveSpeed;
      public boolean showHealthBar;

      public MobVariant(String displayName, String skinName, float moveSpeed, boolean showHealthBar) {
         this.displayName = displayName;
         this.skinName = skinName;
         this.moveSpeed = moveSpeed;
         this.showHealthBar = showHealthBar;
      }
   }
}
