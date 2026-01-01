package com.tlnt.world.entity.decoration.tilemap;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.friend.MapTile;

public final class TileArrayPool extends Pool<Array<MapTile>> {
   public com.tlnt.world.entity.decoration.tilemap.TileMapRenderer renderer;
   public TileArrayPool(TileMapRenderer renderer) {
      super();
      this.renderer = renderer;
   }

   @Override
   public Array<MapTile> newObject() {
      return new com.badlogic.gdx.utils.Array<>();
   }
}
