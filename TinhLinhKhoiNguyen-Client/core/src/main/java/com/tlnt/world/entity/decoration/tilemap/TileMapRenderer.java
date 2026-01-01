package com.tlnt.world.entity.decoration.tilemap;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.friend.MapTile;

public final class TileMapRenderer extends BaseComponent {
   public Pool<Array<MapTile>> lastTemplate = new TileArrayPool(this);
   public Array<Array<MapTile>> projectile = new Array<>();
   public int speed;
   public int active;
   public int autoFlipEnabled;
   public int projectile2;

   public TileMapRenderer(Entity entity) {
      super(entity);
   }

   public final void addTileLayer() {
      this.projectile.add(this.lastTemplate.obtain());
   }

   @Override
   public final void render(RenderContext ctx) {
      OrthographicCamera cam;
      float viewLeft = (cam = ctx.getCurrentCamera()).position.x - cam.viewportWidth * cam.zoom / 2.0F;
      float viewRight = cam.position.x + cam.viewportWidth * cam.zoom / 2.0F;
      float viewBottom = cam.position.y - cam.viewportHeight * cam.zoom / 2.0F;
      float viewTop = cam.position.y + cam.viewportHeight * cam.zoom / 2.0F;
      int colStart = Math.max(0, (int)(viewLeft * 100.0F / (float)this.autoFlipEnabled));
      int colEnd = Math.min(
         this.speed - 1, (int)(viewRight * 100.0F / (float)this.autoFlipEnabled) + 1
      );
      int rowStart = Math.max(0, (int)(viewBottom * 100.0F / (float)this.projectile2));
      int rowEnd = Math.min(
         this.active - 1, (int)(viewTop * 100.0F / (float)this.projectile2) + 1
      );

      for (int layerIdx = 0; layerIdx < this.projectile.size; layerIdx++) {
         Array layer;
         if ((layer = this.projectile.get(layerIdx)) != null) {
            for (int row = rowStart; row <= rowEnd; row++) {
               for (int col = colStart; col <= colEnd; col++) {
                  int tileIndex;
                  MapTile tile;
                  if ((tileIndex = row * this.speed + col) < layer.size && (tile = (MapTile)layer.get(tileIndex)) != null) {
                     ctx.drawTexture(
                        tile.getTextureRegion(), (float)(col * this.autoFlipEnabled) / 100.0F, (float)(row * this.projectile2) / 100.0F, Anchor.BOTTOM_LEFT
                     );
                  }
               }
            }
         }
      }
   }

   public final void clearTileLayers() {
      Array.ArrayIterator iter = this.projectile.iterator();

      while (iter.hasNext()) {
         Array layer;
         (layer = (Array)iter.next()).clear();
         this.lastTemplate.free(layer);
      }

      this.projectile.clear();
   }
}
