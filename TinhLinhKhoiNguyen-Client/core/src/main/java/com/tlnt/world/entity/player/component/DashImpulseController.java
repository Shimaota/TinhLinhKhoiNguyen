package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.physics.box2d.Body;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.world.entity.player.PlayerEntity;

public final class DashImpulseController extends BaseComponent {
   public PlayerEntity projectile;
   public Body speed;
   public boolean active;
   public float autoFlipEnabled;
   public float projectile2;
   public float lastTemplate;

   public DashImpulseController(PlayerEntity player) {
      super(player.getEntity());
      this.projectile = player;
   }

   @Override
   public final void onStart() {
      this.speed = this.projectile.POOL.getBody();
   }

   @Override
   public final void update(float delta) {
      InputHandler.getInstance();
      InputHandler.isKeyJustPressed(30);
      if (this.active) {
         this.autoFlipEnabled += delta;
         if (this.autoFlipEnabled >= this.projectile2) {
            this.active = false;
            return;
         }

         if (this.projectile.isLocalPlayer()) {
            this.speed.setLinearVelocity(this.projectile.getDirection().vector.x * this.lastTemplate * (1.0F - this.autoFlipEnabled / this.projectile2), 0.0F);
         }
      }
   }

   public final void startDash(float duration, float dashSpeed) {
      this.projectile2 = duration;
      this.lastTemplate = dashSpeed;
      this.autoFlipEnabled = 0.0F;
      this.active = true;
   }

   public final boolean isDashing() {
      return this.active;
   }
}
