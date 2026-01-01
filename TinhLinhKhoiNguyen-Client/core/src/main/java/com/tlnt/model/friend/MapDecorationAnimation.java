package com.tlnt.model.friend;

import com.badlogic.gdx.utils.ObjectMap;
import com.tlnt.engine.render.Anchor;

public final class MapDecorationAnimation {
   public static ObjectMap<Integer, MapDecorationAnimation> byId = new ObjectMap<>();
   public int id;
   public String name;
   public float frameDuration;
   public Anchor anchor;
   public String[] frameNames;

   public static void clearAll() {
      byId.clear();
   }

   public static void register(MapDecorationAnimation animation) {
      byId.put(animation.id, animation);
   }

   public static MapDecorationAnimation getById(int id) {
      return byId.get(id);
   }

   public MapDecorationAnimation(int id, String name, int framesPerSecond, int anchorId, String[] frameNames) {
      this.id = id;
      this.name = name;
      this.frameDuration = 1.0F / (float)framesPerSecond;
      this.anchor = Anchor.fromId(anchorId);
      this.frameNames = frameNames;
   }
}
