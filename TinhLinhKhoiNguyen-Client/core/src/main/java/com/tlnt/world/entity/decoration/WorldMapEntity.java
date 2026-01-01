package com.tlnt.world.entity.decoration;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.audio.CollisionFilters;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.BodyType;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.physics.collider.BoxCollider;
import com.tlnt.engine.physics.collider.Collider;
import com.tlnt.engine.physics.collider.PolygonCollider;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.decoration.tilemap.TileMapRenderer;

public final class WorldMapEntity extends GameObject {
   public String POOL2;
   public int skeleton;
   public final BodyComponent movementComponent = new BodyComponent(BodyType.STATIC, this.getEntity());
   public final TileMapRenderer POOL = new TileMapRenderer(this.getEntity());
   public Array<Collider> target2;
   public Array<WaypointEntity> owner;
   public Array<MapDecorationObject> target;

   public WorldMapEntity(LoginScreen screen) {
      super(screen, EntityType.MAP);
      this.POOL.setRenderOrder(1000000.0F);
      this.target2 = new Array<>();
      this.owner = new Array<>();
      this.target = new Array<>();
   }

   public final void addBoxCollider(float x, float y, float width, float height, boolean trigger) {
      if (width == 0.0F) {
         width = 1.0F;
      }

      if (height == 0.0F) {
         height = 1.0F;
      }

      BoxCollider collider;
      (collider = new BoxCollider(width / 2.0F, height / 2.0F, new Vector2(x + width / 2.0F, y + height / 2.0F), this.getEntity()))
         .setFilterData(CollisionFilters.TERRAIN);
      collider.setUserData(trigger ? WorldMapEntity.ColliderType.TRIGGER : WorldMapEntity.ColliderType.SOLID);
      this.target2.add(collider);
   }

   public final void addPolygonCollider(float[] vertices, boolean trigger) {
      PolygonCollider collider;
      (collider = new PolygonCollider(vertices, this.getEntity())).setFilterData(CollisionFilters.TERRAIN);
      collider.setUserData(trigger ? WorldMapEntity.ColliderType.TRIGGER : WorldMapEntity.ColliderType.SOLID);
      this.target2.add(collider);
   }

   public final void clearMap() {
      Array.ArrayIterator iter = this.target2.iterator();

      while (iter.hasNext()) {
         ((Collider)iter.next()).removeFromWorld();
      }

      this.target2.clear(); // the original did not clear it: the collision array piled up across map changes, and next time old collisions were removed again

      this.POOL.clearTileLayers();
      iter = this.owner.iterator();

      while (iter.hasNext()) {
         ((WaypointEntity)iter.next()).removeFromWorld();
      }

      this.owner.clear();

      for (MapDecorationObject decoration : this.target) {
         MapDecorationObject.POOL2.free(decoration);
      }

      this.target.clear();
   }

   public final float getMapWidth() {
      return (float)(this.POOL.speed * this.POOL.autoFlipEnabled);
   }

   public final float getMapHeight() {
      return (float)(this.POOL.active * this.POOL.projectile2);
   }

   @Override
   public final void dispose() {
   }

   public static enum ColliderType {
      SOLID,
      TRIGGER;

   }
}
