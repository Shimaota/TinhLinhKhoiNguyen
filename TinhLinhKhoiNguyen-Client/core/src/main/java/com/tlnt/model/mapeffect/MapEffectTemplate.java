package com.tlnt.model.mapeffect;

import com.badlogic.gdx.utils.IntMap;

/**
 * Floating object template (trailing section of the map data file — table {@code tlnt_data.map_effect}, decompiled C3960): id, name,
 * skeleton path (e.g. 1 "Hoa đăng" = {@code skeletons/vfxs/lotus_lantern}). Used by op 80 ({@code FloatAwayEffect}).
 */
public final class MapEffectTemplate {
   private static final IntMap<MapEffectTemplate> BY_ID = new IntMap<>();

   public final int id;
   public final String name;
   public final String skeletonPath;

   public MapEffectTemplate(int id, String name, String skeletonPath) {
      this.id = id;
      this.name = name;
      this.skeletonPath = skeletonPath;
   }

   public static void register(MapEffectTemplate template) {
      BY_ID.put(template.id, template);
   }

   public static MapEffectTemplate getById(int id) {
      return BY_ID.get(id);
   }

   public static void clearAll() {
      BY_ID.clear();
   }
}
