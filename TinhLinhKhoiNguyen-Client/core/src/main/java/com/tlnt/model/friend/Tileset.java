package com.tlnt.model.friend;

import com.badlogic.gdx.utils.ObjectMap;

public final class Tileset {
   public static ObjectMap<Integer, Tileset> byId = new ObjectMap<>();
   public int id;
   public String textureName;
   public int tileWidth;
   public int tileHeight;
   public MapTile[] tiles;

   public static void clearAll() {
      byId.clear();
   }

   public static void register(Tileset tileset) {
      byId.put(tileset.id, tileset);
   }

   public Tileset(int id, String textureName, int tileWidth, int tileHeight) {
      this.id = id;
      this.textureName = textureName;
      this.tileWidth = tileWidth;
      this.tileHeight = tileHeight;
   }
}
