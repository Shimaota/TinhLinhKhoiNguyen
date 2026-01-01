package com.tlnt.engine.physics.collider;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Entity;

public class PolygonCollider extends Collider {
   public final PolygonShape autoFlipEnabled;
   public float[] projectile2;
   public float lastTemplate;
   public float height;
   public Vector2 offset;

   public PolygonCollider(float[] vertices, Entity entity) {
      this(Vector2.Zero, vertices, entity);
   }

   public PolygonCollider(Vector2 offset, float[] vertices, Entity entity) {
      this(offset, vertices, new FixtureDef(), entity);
   }

   public PolygonCollider(Vector2 offset, float[] vertices, FixtureDef fixtureDef, Entity entity) {
      super(fixtureDef, entity);
      this.offset = offset.cpy().scl(0.01F);
      this.projectile2 = new float[vertices.length];

      for (int i = 0; i < vertices.length; i += 2) {
         this.projectile2[i] = vertices[i] / 100.0F + this.offset.x;
         int yIndex = i + 1;
         this.projectile2[yIndex] = vertices[i + 1] / 100.0F + this.offset.y;
      }

      this.autoFlipEnabled = new PolygonShape();
      this.autoFlipEnabled.set(this.projectile2);
      PolygonCollider self = this;
      float minX = Float.MAX_VALUE;
      float maxX = Float.MIN_VALUE;
      float minY = Float.MAX_VALUE;
      float maxY = Float.MIN_VALUE;

      for (int j = 0; j < self.projectile2.length; j += 2) {
         float x = self.projectile2[j];
         float y = self.projectile2[j + 1];
         if (x < minX) {
            minX = x;
         }

         if (x > maxX) {
            maxX = x;
         }

         if (y < minY) {
            minY = y;
         }

         if (y > maxY) {
            maxY = y;
         }
      }

      self.lastTemplate = maxX - minX;
      self.height = maxY - minY;
   }

   @Override
   public final void onAdded() {
      super.onAdded();
      this.getEntity().getComponent(BodyComponent.class).setBoundsSize(this.lastTemplate, this.height);
      this.createFixture(this.autoFlipEnabled);
   }
}
