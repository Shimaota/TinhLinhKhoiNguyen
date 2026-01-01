package com.tlnt.world.entity.mapprop;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;

/**
 * Decoration placed on the map by the server (op 81 — port of decompiled C3531 / C0092 / C3422): one {@code texturePath} image drawn at the
 * foot (bottom center) of the entity position, draw layer 5,500,000. Sub 0 list, 1 add one, 2 remove by id. Everything is cleared on map change.
 */
public final class MapPropEntity extends GameObject {
   public static final Array<MapPropEntity> ACTIVE = new Array<>();
   private static final ObjectPool<MapPropEntity> POOL = new ObjectPool<MapPropEntity>() {
      @Override
      public MapPropEntity newObject() {
         return new MapPropEntity(GameMain.instance.getLoginScreen());
      }
   };

   /** Created right in the constructor (onAwake runs one tick late — the first object used to not show up). */
   final PropRenderer renderer;

   public MapPropEntity(LoginScreen screen) {
      super(screen);
      this.renderer = new PropRenderer(this);
   }

   public static void spawn(int id, String texturePath, float x, float y) {
      MapPropEntity prop = POOL.obtain();
      prop.setActive(true);
      prop.setId(id);
      prop.teleportTo(x, y);
      prop.renderer.texturePath = texturePath;
      ACTIVE.add(prop);
   }

   public static void remove(int id) {
      for (int i = ACTIVE.size - 1; i >= 0; i--) {
         MapPropEntity prop = ACTIVE.get(i);
         if (prop.getId() == id) {
            free(prop);
         }
      }
   }

   static void free(MapPropEntity prop) {
      ACTIVE.removeValue(prop, true);
      prop.renderer.texturePath = null;
      prop.setActive(false);
      POOL.free(prop);
   }

   public static void clearAll() {
      while (ACTIVE.size > 0) {
         free(ACTIVE.peek());
      }
   }

   @Override
   public final void dispose() {
   }

   static final class PropRenderer extends BaseComponent {
      private final MapPropEntity prop;
      String texturePath;

      PropRenderer(MapPropEntity prop) {
         super(prop.getEntity());
         this.prop = prop;
         this.setRenderOrder(5500000.0F);
      }

      @Override
      public final void render(RenderContext ctx) {
         if (this.texturePath == null) {
            return;
         }
         ctx.drawTexture(TextureAsset.obtain(this.texturePath), this.prop.getPosition().x, this.prop.getPosition().y, Anchor.BOTTOM_CENTER);
      }
   }
}
