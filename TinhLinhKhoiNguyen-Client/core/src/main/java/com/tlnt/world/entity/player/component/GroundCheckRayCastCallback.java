package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.tlnt.world.entity.decoration.WorldMapEntity;

public final class GroundCheckRayCastCallback implements RayCastCallback {
   public com.tlnt.world.entity.player.component.PlayerMovementController playerMovementController;
   public GroundCheckRayCastCallback(PlayerMovementController playerMovementController) {
      super();
      this.playerMovementController = playerMovementController;
   }

   @Override
   public final float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
      Object userData;
      if ((userData = fixture.getUserData()) instanceof WorldMapEntity.ColliderType) {
         int isTrigger;
         if (userData == WorldMapEntity.ColliderType.TRIGGER) {
            isTrigger = 1;
         } else {
            isTrigger = 0;
         }

         this.playerMovementController.projectile = (isTrigger != 0);
         this.playerMovementController.autoFlipEnabled = true;
         return 0.0F;
      } else {
         return 1.0F;
      }
   }
}
