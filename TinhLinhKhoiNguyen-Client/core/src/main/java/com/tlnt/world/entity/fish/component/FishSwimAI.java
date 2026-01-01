package com.tlnt.world.entity.fish.component;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameMain;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Direction;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.decoration.WorldMapEntity;
import com.tlnt.world.entity.fish.AmbientFish;

public final class FishSwimAI extends BaseComponent {
   public AmbientFish projectile;
   public Vector2 speed = new Vector2();
   public float active;
   public float autoFlipEnabled;
   public float projectile2;
   public float lastTemplate;
   public float waveAmplitude;
   public float waveFrequency;
   public int currentPhase = FishSwimAI.SwimPhase.SWIMMING;
   public float phaseTimer;

   public FishSwimAI(AmbientFish fish) {
      super(fish.getEntity());
      this.projectile = fish;
      this.active = GameUtils.randomFloat(0.25F, 0.45F);
      this.autoFlipEnabled = this.active * GameUtils.randomFloat(2.5F, 3.5F);
      this.projectile2 = GameUtils.randomFloat(1.5F, 3.0F);
      this.waveAmplitude = GameUtils.randomFloat(0.15F, 0.35F);
      this.waveFrequency = GameUtils.randomFloat(2.5F, 4.5F);
      this.lastTemplate = GameUtils.randomFloat(0.0F, 100.0F);
      this.speed.set(GameUtils.rollChance(1, 2) ? 1.0F : -1.0F, 0.0F).scl(this.active);
      this.resetPhaseTimer();
   }

   @Override
   public final void update(float delta) {
      Vector2 pos = this.projectile.getPosition().cpy();
      this.lastTemplate += delta;
      this.phaseTimer -= delta;
      if (this.phaseTimer <= 0.0F) {
         if (this.currentPhase == FishSwimAI.SwimPhase.SWIMMING) {
            this.currentPhase = FishSwimAI.SwimPhase.TURNING;
            this.phaseTimer = GameUtils.randomFloat(0.4F, 1.1F);
            if (GameUtils.rollChance(30, 100)) {
               this.speed.x = -this.speed.x;
            }
         } else {
            this.currentPhase = FishSwimAI.SwimPhase.SWIMMING;
            this.resetPhaseTimer();
         }
      }

      float targetSpeed = this.currentPhase == FishSwimAI.SwimPhase.TURNING ? this.autoFlipEnabled : this.active;
      targetSpeed = Math.signum(this.speed.x) * targetSpeed - this.speed.x;
      targetSpeed = Math.max(-this.projectile2 * delta, Math.min(this.projectile2 * delta, targetSpeed));
      this.speed.x += targetSpeed;
      this.speed.y = (float)Math.sin((double)(this.lastTemplate * this.waveFrequency)) * this.waveAmplitude;
      pos.mulAdd(this.speed, delta);
      Rectangle bounds = this.projectile.POOL2;
      float minY;
      float maxY;
      if (this.projectile.POOL2 != null) {
         delta = bounds.x;
         targetSpeed = bounds.x + bounds.width;
         minY = bounds.y;
         maxY = bounds.y + bounds.height;
      } else {
         WorldMapEntity worldMap = GameMain.instance.getLoginScreen().editorViewport;
         delta = 0.0F;
         targetSpeed = worldMap.getMapWidth() / 100.0F;
         minY = 0.0F;
         maxY = worldMap.getMapHeight() / 100.0F;
      }

      if (pos.x < delta) {
         pos.x = delta;
         this.speed.x = Math.abs(this.speed.x);
      } else if (pos.x > targetSpeed) {
         pos.x = targetSpeed;
         this.speed.x = -Math.abs(this.speed.x);
      }

      if (pos.y < minY) {
         pos.y = minY;
      }

      if (pos.y > maxY) {
         pos.y = maxY;
      }

      this.projectile.setWorldPosition(pos);
      this.projectile.setDirection(this.speed.x > 0.0F ? Direction.RIGHT : Direction.LEFT);
   }

   public void resetPhaseTimer() {
      this.phaseTimer = GameUtils.randomFloat(2.0F, 5.0F);
   }

   public static final class SwimPhase {
      public static final int SWIMMING = 1;
      public static final int TURNING;

      static {
         TURNING = 2;
         int[] values = new int[2];
         values[0] = SWIMMING;
         values[1] = TURNING;
      }
   }
}
