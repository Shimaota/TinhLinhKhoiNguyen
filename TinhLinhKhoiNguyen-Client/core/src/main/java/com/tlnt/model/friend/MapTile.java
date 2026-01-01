package com.tlnt.model.friend;

import com.badlogic.gdx.utils.ObjectMap;
import com.tlnt.engine.render.TextureAsset;

public final class MapTile {
   public static ObjectMap<Integer, MapTile> byId = new ObjectMap<>();
   public Tileset tileset;
   public int id;
   public byte type;
   public int srcX;
   public int srcY;
   public MapTile.AnimFrame[] animFrames;
   public TextureAsset textureRegion;

   public static void clearAll() {
      byId.clear();
   }

   public static void register(MapTile tile) {
      byId.put(tile.id, tile);
   }

   public static MapTile getById(int id) {
      return byId.get(id);
   }

   public MapTile(Tileset tileset, int id, byte type) {
      this.tileset = tileset;
      this.id = id;
      this.type = type;
   }

   public final TextureAsset getTextureRegion() {
      if (this.textureRegion == null) {
         this.textureRegion = new TextureAsset(
            TextureAsset.obtain(this.tileset.textureName), this.srcX, this.srcY, this.tileset.tileWidth, this.tileset.tileHeight
         );
      }

      return this.textureRegion;
   }

   public static class AnimFrame {
      public int tileId;
      public int duration;

      public AnimFrame(int tileId, int duration) {
         this.tileId = tileId;
         this.duration = duration;
      }
   }
}
