package com.tlnt.world.entity.mob.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.tlnt.world.entity.decoration.WorldMapEntity;

public final class MobTerrainRayCallback implements RayCastCallback {
   public boolean[] hitResult;
   public com.tlnt.world.entity.mob.component.MobWanderAI wanderAI;
   public MobTerrainRayCallback(MobWanderAI wanderAI, boolean[] hitResult) {
      super();
      this.wanderAI = wanderAI;
      this.hitResult = hitResult;
   }

   @Override
   public final float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
      if (fixture.getUserData() != null && fixture.getUserData() instanceof WorldMapEntity.ColliderType) {
         this.hitResult[0] = true;
         return 0.0F;
      } else {
         return -1.0F;
      }
   }
}
