package com.tlnt.world.skeleton;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.tlnt.world.entity.decoration.WorldMapEntity;

public final class ShadowGroundRayCast implements RayCastCallback {
   public com.badlogic.gdx.math.Vector2 startPoint;
   public com.badlogic.gdx.math.Vector2 hitPoint;
   public com.tlnt.world.skeleton.GroundShadowRenderer shadowRenderer;
   public ShadowGroundRayCast(GroundShadowRenderer shadowRenderer, Vector2 hitPoint, Vector2 startPoint) {
      super();
      this.shadowRenderer = shadowRenderer;
      this.hitPoint = hitPoint;
      this.startPoint = startPoint;
   }

   @Override
   public final float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
      if (!(fixture.getUserData() instanceof WorldMapEntity.ColliderType)) {
         return 1.0F;
      } else {
         if (this.hitPoint.x == -1.0F || point.dst2(this.startPoint) < this.hitPoint.dst2(this.startPoint)) {
            this.shadowRenderer.projectile.set(point);
            this.hitPoint.set(point);
         }

         return fraction;
      }
   }
}
