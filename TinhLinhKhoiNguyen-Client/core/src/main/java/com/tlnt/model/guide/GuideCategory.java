package com.tlnt.model.guide;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

public final class GuideCategory implements Disposable {
   public static boolean dirty;
   public static ObjectMap<Integer, GuideCategory> byId = new ObjectMap<>();
   public static Array<GuideCategory> all = new Array<>();
   public int id;
   public String name;
   public String iconPath;
   public Array<GuideTopic> topics = new Array<>();

   public static void register(GuideCategory category) {
      byId.put(category.id, category);
      all.add(category);
   }

   public static void clearAll() {
      byId.clear();
      all.clear();
   }

   public static Array<GuideCategory> getAll() {
      return all;
   }

   public static GuideCategory getOrCreate(int id) {
      if (!byId.containsKey(id)) {
         byId.put(id, new GuideCategory());
      }

      return byId.get(id);
   }

   @Override
   public final void dispose() {
      this.name = null;
   }
}
