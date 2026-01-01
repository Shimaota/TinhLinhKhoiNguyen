package com.tlnt.world.entity.animal.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.tlnt.world.entity.decoration.WorldMapEntity;

public final class AnimalFloorRayCallback implements RayCastCallback {
   public com.tlnt.world.entity.animal.component.AnimalWanderAI wanderAI;
   public boolean[] floorHit;
   public AnimalFloorRayCallback(AnimalWanderAI wanderAI, boolean[] floorHit) {
      super();
      this.wanderAI = wanderAI;
      this.floorHit = floorHit;
   }

   @Override
   public final float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
      if (fixture.getUserData() != null && fixture.getUserData() instanceof WorldMapEntity.ColliderType) {
         this.floorHit[0] = true;
         return 0.0F;
      } else {
         return -1.0F;
      }
   }
}
