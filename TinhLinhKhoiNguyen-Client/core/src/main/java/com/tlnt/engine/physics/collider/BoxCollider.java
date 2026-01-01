package com.tlnt.engine.physics.collider;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Entity;

public class BoxCollider extends Collider {
   public final PolygonShape autoFlipEnabled;
   public float projectile2;
   public float lastTemplate;
   public Vector2 offset;

   public BoxCollider(float halfWidth, float halfHeight, Vector2 offset, Entity entity) {
      this(halfWidth, halfHeight, offset, new FixtureDef(), entity);
   }

   public BoxCollider(float halfWidth, float halfHeight, Vector2 offset, FixtureDef fixtureDef, Entity entity) {
      super(fixtureDef, entity);
      halfWidth /= 100.0F;
      halfHeight /= 100.0F;
      this.projectile2 = halfWidth * 2.0F;
      this.lastTemplate = halfHeight * 2.0F;
      this.offset = offset.cpy().scl(0.01F);
      this.autoFlipEnabled = new PolygonShape();
      this.autoFlipEnabled.setAsBox(halfWidth, halfHeight);
   }

   @Override
   public final void onAdded() {
      super.onAdded();
      this.getEntity().getComponent(BodyComponent.class).setBoundsSize(this.projectile2, this.lastTemplate);
      this.createFixtureWithOffset(this.autoFlipEnabled, this.offset);
   }
}
