package com.tlnt.engine.physics.collider;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.tlnt.engine.physics.Entity;

public class CircleCollider extends Collider {
   public float autoFlipEnabled;
   public Vector2 projectile2;

   public CircleCollider(Vector2 offset, float radius, Entity entity) {
      this(offset, 25.0F, new FixtureDef(), entity);
   }

   public CircleCollider(Vector2 offset, float radius, FixtureDef fixtureDef, Entity entity) {
      super(fixtureDef, entity);
      this.autoFlipEnabled = radius / 100.0F;
      this.projectile2 = offset.cpy().scl(0.01F);
   }

   public final float getRadius() {
      return this.autoFlipEnabled;
   }

   @Override
   public final void onAdded() {
      super.onAdded();
      CircleShape shape;
      (shape = new CircleShape()).setRadius(this.autoFlipEnabled);
      this.createFixtureWithOffset(shape, this.projectile2);
   }
}
