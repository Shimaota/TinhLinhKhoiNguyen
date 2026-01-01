package com.tlnt.engine.physics.collider;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Entity;

public final class CapsuleCollider extends Collider {
   public final CircleShape autoFlipEnabled;
   public final CircleShape projectile2;
   public final EdgeShape lastTemplate;
   public final EdgeShape secondEdgeShape;
   public final boolean horizontal;
   public float width;
   public float height;
   public Vector2 offset;
   public Array<Fixture> fixtures = new Array<>();

   @Override
   public final void onStart() {
      Array.ArrayIterator it = this.fixtures.iterator();

      while (it.hasNext()) {
         Fixture fixture;
         (fixture = (Fixture)it.next()).setSensor(super.speed);
         fixture.setUserData(super.active == null ? this.getEntity() : super.active);
         if (super.projectile != null) {
            fixture.setFilterData(super.projectile);
         }
      }
   }

   public CapsuleCollider(Vector2 offset, float width, float height, Entity entity) {
      this(offset, 70.0F, 150.0F, new FixtureDef(), entity);
   }

   public CapsuleCollider(Vector2 offset, float width, float height, FixtureDef fixtureDef, Entity entity) {
      super(fixtureDef, entity);
      width /= 100.0F;
      height /= 100.0F;
      this.offset = offset.cpy().scl(0.01F);
      int horizontalFlag;
      if (width > height) {
         horizontalFlag = 1;
      } else {
         horizontalFlag = 0;
      }

      this.horizontal = (horizontalFlag != 0);
      float radius = Math.min(width, height) / 2.0F;
      this.width = width;
      this.height = height;
      if (this.horizontal) {
         this.autoFlipEnabled = new CircleShape();
         this.autoFlipEnabled.setRadius(radius);
         this.autoFlipEnabled.setPosition(new Vector2(-(width / 2.0F - radius), 0.0F).add(this.offset));
         this.projectile2 = new CircleShape();
         this.projectile2.setRadius(radius);
         this.projectile2.setPosition(new Vector2(width / 2.0F - radius, 0.0F).add(this.offset));
         this.lastTemplate = new EdgeShape();
         this.lastTemplate.set(new Vector2(-(width / 2.0F - radius), radius).add(this.offset), new Vector2(width / 2.0F - radius, radius).add(this.offset));
         this.secondEdgeShape = new EdgeShape();
         this.secondEdgeShape.set(new Vector2(-(width / 2.0F - radius), -radius).add(this.offset), new Vector2(width / 2.0F - radius, -radius).add(this.offset));
      } else {
         this.autoFlipEnabled = new CircleShape();
         this.autoFlipEnabled.setRadius(radius);
         this.autoFlipEnabled.setPosition(new Vector2(0.0F, height / 2.0F - radius).add(this.offset));
         this.projectile2 = new CircleShape();
         this.projectile2.setRadius(radius);
         this.projectile2.setPosition(new Vector2(0.0F, -(height / 2.0F - radius)).add(this.offset));
         this.lastTemplate = new EdgeShape();
         this.lastTemplate.set(new Vector2(-radius, height / 2.0F - radius).add(this.offset), new Vector2(-radius, -(height / 2.0F - radius)).add(this.offset));
         this.secondEdgeShape = new EdgeShape();
         this.secondEdgeShape.set(new Vector2(radius, height / 2.0F - radius).add(this.offset), new Vector2(radius, -(height / 2.0F - radius)).add(this.offset));
      }
   }

   @Override
   public final void onAdded() {
      super.onAdded();
      this.getEntity().getComponent(BodyComponent.class).setBoundsSize(this.width, this.height);
      this.fixtures.add(this.createFixture(this.autoFlipEnabled));
      this.fixtures.add(this.createFixture(this.projectile2));
      this.fixtures.add(this.createFixture(this.lastTemplate));
      this.fixtures.add(this.createFixture(this.secondEdgeShape));
   }

   @Override
   public final Collider setUserData(Object userData) {
      super.setUserData(userData);
      java.util.Iterator it = this.fixtures.iterator();

      while (it.hasNext()) {
         Fixture fixture;
         (fixture = (Fixture)it.next()).setSensor(super.speed);
         fixture.setUserData(super.active == null ? this.getEntity() : super.active);
         if (super.projectile != null) {
            fixture.setFilterData(super.projectile);
         }
      }

      return this;
   }

   @Override
   public final Collider setFilterData(Filter filter) {
      super.setFilterData(filter);
      Array.ArrayIterator it = this.fixtures.iterator();

      while (it.hasNext()) {
         Fixture fixture;
         (fixture = (Fixture)it.next()).setSensor(super.speed);
         fixture.setUserData(super.active == null ? this.getEntity() : super.active);
         if (super.projectile != null) {
            fixture.setFilterData(super.projectile);
         }
      }

      return this;
   }

   @Override
   public final Collider setSensor(boolean sensor) {
      super.setSensor(sensor);
      Array.ArrayIterator it = this.fixtures.iterator();

      while (it.hasNext()) {
         Fixture fixture;
         (fixture = (Fixture)it.next()).setSensor(super.speed);
         fixture.setUserData(super.active == null ? this.getEntity() : super.active);
         if (super.projectile != null) {
            fixture.setFilterData(super.projectile);
         }
      }

      return this;
   }

   @Override
   public final boolean testPoint(Vector2 point) {
      Array.ArrayIterator it = this.fixtures.iterator();

      while (it.hasNext()) {
         if (((Fixture)it.next()).testPoint(point)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public final void onDestroy() {
      BodyComponent bodyComponent;
      if ((bodyComponent = this.getEntity().getBodyComponent()) != null) {
         for (Fixture fixture : this.fixtures) {
            bodyComponent.getBody().destroyFixture(fixture);
         }
      }
   }
}
