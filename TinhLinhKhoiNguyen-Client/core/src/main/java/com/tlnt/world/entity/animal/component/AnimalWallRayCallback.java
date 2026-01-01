package com.tlnt.world.entity.animal.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.tlnt.world.entity.decoration.WorldMapEntity;

public final class AnimalWallRayCallback implements RayCastCallback {
   public com.tlnt.world.entity.animal.component.AnimalWanderAI wanderAI;
   public boolean[] wallHit;
   public AnimalWallRayCallback(AnimalWanderAI wanderAI, boolean[] wallHit) {
      super();
      this.wanderAI = wanderAI;
      this.wallHit = wallHit;
   }

   @Override
   public final float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
      if (fixture.getUserData() != null && fixture.getUserData() instanceof WorldMapEntity.ColliderType) {
         this.wallHit[0] = true;
         return 0.0F;
      } else {
         return -1.0F;
      }
   }
}
