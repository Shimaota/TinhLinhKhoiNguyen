package com.tlnt.engine.physics.collider;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Entity;

public abstract class Collider extends BaseComponent {
   public Fixture autoFlipEnabled;
   public final FixtureDef projectile2;
   public Filter projectile;
   public boolean speed;
   public Object active;
   public PhysicsMaterial lastTemplate;

   public Collider(FixtureDef fixtureDef, Entity entity) {
      super(entity);
      this.projectile2 = fixtureDef;
   }

   @Override
   public void onStart() {
      this.autoFlipEnabled.setUserData(this.active == null ? this.getEntity() : this.active);
      this.autoFlipEnabled.setSensor(this.speed);
      if (this.projectile != null) {
         this.autoFlipEnabled.setFilterData(this.projectile);
      }
   }

   public final Fixture createFixture(Shape shape) {
      BodyComponent bodyComponent = this.getEntity().getComponent(BodyComponent.class);
      this.projectile2.shape = shape;
      this.autoFlipEnabled = bodyComponent.getBody().createFixture(this.projectile2);
      if (this.lastTemplate != null) {
         this.autoFlipEnabled.setFriction(this.lastTemplate.friction);
         this.autoFlipEnabled.setRestitution(this.lastTemplate.restitution);
         this.autoFlipEnabled.setDensity(this.lastTemplate.density);
      }

      shape.dispose();
      return this.autoFlipEnabled;
   }

   public final Fixture createFixtureWithOffset(Shape shape, Vector2 offset) {
      BodyComponent bodyComponent = this.getEntity().getComponent(BodyComponent.class);
      this.projectile2.shape = shape;
      if (shape instanceof CircleShape) {
         ((CircleShape)shape).setPosition(offset);
      } else if (shape instanceof PolygonShape) {
         PolygonShape polygon;
         Vector2[] vertices = new Vector2[(polygon = (PolygonShape)shape).getVertexCount()];

         for (int i = 0; i < polygon.getVertexCount(); i++) {
            Vector2 vertex = new Vector2();
            polygon.getVertex(i, vertex);
            vertices[i] = vertex.add(offset);
         }

         polygon.set(vertices);
      }

      this.autoFlipEnabled = bodyComponent.getBody().createFixture(this.projectile2);
      if (this.lastTemplate != null) {
         this.autoFlipEnabled.setFriction(this.lastTemplate.friction);
         this.autoFlipEnabled.setRestitution(this.lastTemplate.restitution);
         this.autoFlipEnabled.setDensity(this.lastTemplate.density);
      }

      shape.dispose();
      return this.autoFlipEnabled;
   }

   public Collider setUserData(Object userData) {
      this.active = userData;
      if (this.autoFlipEnabled != null) {
         this.autoFlipEnabled.setUserData(this.active == null ? this.getEntity() : this.active);
      }

      return this;
   }

   public Collider setFilterData(Filter filter) {
      this.projectile = filter;
      if (this.autoFlipEnabled != null && this.projectile != null) {
         this.autoFlipEnabled.setFilterData(this.projectile);
      }

      return this;
   }

   public Collider setSensor(boolean sensor) {
      this.speed = sensor;
      if (this.autoFlipEnabled != null) {
         this.autoFlipEnabled.setSensor(this.speed);
      }

      return this;
   }

   public final void setMaterial(PhysicsMaterial material) {
      this.lastTemplate = material;
      if (this.autoFlipEnabled != null) {
         this.autoFlipEnabled.setDensity(this.lastTemplate.density);
         this.autoFlipEnabled.setFriction(this.lastTemplate.friction);
         this.autoFlipEnabled.setRestitution(this.lastTemplate.restitution);
      }
   }

   public boolean testPoint(Vector2 point) {
      return this.autoFlipEnabled.testPoint(point);
   }

   @Override
   public void onDestroy() {
      BodyComponent bodyComponent;
      if ((bodyComponent = this.getEntity().getBodyComponent()) != null) {
         bodyComponent.getBody().destroyFixture(this.autoFlipEnabled);
      }
   }
}
