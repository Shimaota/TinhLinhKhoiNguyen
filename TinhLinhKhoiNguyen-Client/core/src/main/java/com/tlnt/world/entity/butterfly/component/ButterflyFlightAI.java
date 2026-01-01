package com.tlnt.world.entity.butterfly.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameMain;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Direction;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.butterfly.AmbientButterfly;
import com.tlnt.world.entity.decoration.WorldMapEntity;

public final class ButterflyFlightAI extends BaseComponent {
   public AmbientButterfly projectile;
   public Vector2 speed = new Vector2();
   public Vector2 active = new Vector2();
   public float autoFlipEnabled = 1.0F;
   public float projectile2 = 1.0F;
   public float lastTemplate = 1.5F;
   public float forwardSpeed = 2.0F;
   public float wanderJitter = 0.8F;
   public Vector2 wanderDirection = new Vector2(1.0F, 0.0F);

   public ButterflyFlightAI(AmbientButterfly butterfly) {
      super(butterfly.getEntity());
      this.projectile = butterfly;
   }

   @Override
   public final void update(float delta) {
      Vector2 pos = this.projectile.getPosition().cpy();
      if (this.speed.isZero() || pos.epsilonEquals(this.speed, 0.3F)) {
         WorldMapEntity worldMap = GameMain.instance.getLoginScreen().editorViewport;
         this.speed.set(GameUtils.randomFloat(0.0F, worldMap.getMapWidth() / 100.0F), GameUtils.randomFloat(0.0F, worldMap.getMapHeight() / 100.0F));
      }

      this.wanderDirection.add(GameUtils.randomFloat(-1.0F, 1.0F) * this.wanderJitter, GameUtils.randomFloat(-1.0F, 1.0F) * this.wanderJitter);
      this.wanderDirection.nor().scl(this.lastTemplate);
      Vector2 steer;
      if ((steer = new Vector2(this.active)).isZero()) {
         steer.set(1.0F, 0.0F);
      }

      steer.nor().scl(this.forwardSpeed);
      steer = new Vector2(steer).add(this.wanderDirection);
      Vector2 desiredVel;
      (desiredVel = new Vector2(this.speed).sub(pos).nor().scl(this.autoFlipEnabled)).add(steer).nor().scl(this.autoFlipEnabled);
      (steer = desiredVel.sub(this.active)).limit(this.projectile2 * delta);
      this.active.add(steer);
      this.active.limit(this.autoFlipEnabled);
      pos.add(new Vector2(this.active).scl(delta));
      this.projectile.setWorldPosition(pos);
      if (Math.abs(this.active.x) > 0.01F) {
         if (this.active.x > 0.0F) {
            this.projectile.setDirection(Direction.RIGHT);
            return;
         }

         this.projectile.setDirection(Direction.LEFT);
      }
   }
}
