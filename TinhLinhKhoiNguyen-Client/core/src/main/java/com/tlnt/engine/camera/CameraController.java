package com.tlnt.engine.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;

public final class CameraController extends GameObject {
   public final OrthographicCamera POOL2;
   public boolean skeleton;
   public float POOL;
   public float owner;
   public GameObject target;
   public final Vector2 movementComponent = new Vector2();
   public final Vector2 target2 = new Vector2(0.0F, 0.0F);
   public final Vector2 targetPosition = new Vector2(0.0F, 0.0F);
   public float shakeTimeLeft = 0.0F;
   public float shakeDuration = 0.0F;
   public float shakeAmplitudeX = 0.0F;
   public float shakeAmplitudeY = 0.0F;
   public final Vector2 shakeOffset = new Vector2();

   public CameraController(LoginScreen screen) {
      super(screen);
      this.POOL2 = screen.getWorldCamera();
   }

   public final void setFocusPoint(float x, float y) {
      this.movementComponent.set(x, y);
   }

   @Override
   public final void update(float delta) {
      if (this.target == null) {
         this.updateShake(delta);
         if (this.shakeOffset.isZero()) {
            return;
         }

         this.POOL2.position.add(this.shakeOffset.x, this.shakeOffset.y, 0.0F);
         this.POOL2.position.x = (float)Math.round(this.POOL2.position.x * 1000.0F) / 1000.0F;
         this.POOL2.position.y = (float)Math.round(this.POOL2.position.y * 1000.0F) / 1000.0F;
      } else {
         LoginScreen screen;
         float mapWidth = (screen = (LoginScreen)this.getScreen()).editorViewport.getMapWidth();
         float mapHeight = screen.editorViewport.getMapHeight();
         float viewWidth = this.POOL2.viewportWidth * 100.0F * this.POOL2.zoom;
         float viewHeight = this.POOL2.viewportHeight * 100.0F * this.POOL2.zoom;
         Vector2 direction = this.target.getDirection().vector;
         this.target2.lerp(direction, 0.1F);
         float focusX = this.target.getPosition().x * 100.0F;
         float focusY = (this.target.getPosition().y + this.target.getHeight() / 2.0F) * 100.0F;
         if (!this.movementComponent.isZero()) {
            focusX = this.movementComponent.x * 100.0F;
            focusY = this.movementComponent.y * 100.0F;
            this.movementComponent.setZero();
         }

         float lookAheadX = focusX + this.target2.x * 100.0F;
         focusY += this.target2.y * 100.0F;
         float camX;
         if (mapWidth <= viewWidth) {
            camX = mapWidth / 2.0F;
         } else {
            float halfWidth;
            camX = Math.max(halfWidth = viewWidth / 2.0F, Math.min(lookAheadX, mapWidth - halfWidth));
         }

         if (mapHeight <= viewHeight) {
            focusY = mapHeight / 2.0F;
         } else {
            focusY = Math.max(viewWidth = viewHeight / 2.0F, Math.min(focusY, mapHeight - viewWidth));
         }

         this.targetPosition.set(camX / 100.0F, focusY / 100.0F);
         if (this.skeleton) {
            this.skeleton = false;
            this.POOL2.position.set(this.POOL, this.owner, 0.0F);
         } else {
            float unused = MathUtils.clamp(this.POOL2.position.dst(this.targetPosition.x, this.targetPosition.y, 0.0F) * 2.0F, 0.02F, 1.0F) / 50.0F;
            this.POOL2.position.lerp(new Vector3(this.targetPosition.x, this.targetPosition.y, 0.0F), delta * 2.0F);
            this.POOL2.position.x = (float)Math.round(this.POOL2.position.x * 1000.0F) / 1000.0F;
            this.POOL2.position.y = (float)Math.round(this.POOL2.position.y * 1000.0F) / 1000.0F;
         }

         this.updateShake(delta);
         if (!this.shakeOffset.isZero()) {
            this.POOL2.position.add(this.shakeOffset.x, this.shakeOffset.y, 0.0F);
         }
      }

      this.POOL2.update();
   }

   public final void startShake(float duration, float intensity) {
      float amplitudeY = 0.05F;
      float amplitudeX = 0.05F;
      if (!(duration <= 0.0F)) {
         this.shakeTimeLeft = duration;
         this.shakeDuration = duration;
         this.shakeAmplitudeX = Math.abs(amplitudeX);
         this.shakeAmplitudeY = Math.abs(amplitudeY);
      }
   }

   public void updateShake(float delta) {
      if (this.shakeTimeLeft <= 0.0F) {
         this.shakeOffset.setZero();
      } else {
         this.shakeTimeLeft -= delta;
         if (this.shakeTimeLeft < 0.0F) {
            this.shakeTimeLeft = 0.0F;
         }

         delta = (delta = this.shakeDuration > 0.0F ? this.shakeTimeLeft / this.shakeDuration : 0.0F) * delta;
         float randomX = MathUtils.random(-1.0F, 1.0F);
         float randomY = MathUtils.random(-1.0F, 1.0F);
         this.shakeOffset.x = randomX * this.shakeAmplitudeX * delta;
         this.shakeOffset.y = randomY * this.shakeAmplitudeY * delta;
      }
   }

   public final void setFollowTarget(PlayerEntity player) {
      this.target = player;
   }

   public final void snapToPosition(float x, float y) {
      LoginScreen screen;
      float mapWidth = (screen = (LoginScreen)this.getScreen()).editorViewport.getMapWidth();
      float mapHeightPx = screen.editorViewport.getMapHeight();
      float viewWidth = this.POOL2.viewportWidth * this.POOL2.zoom;
      float viewHeight = this.POOL2.viewportHeight * this.POOL2.zoom;
      mapWidth /= 100.0F;
      float mapHeight = mapHeightPx / 100.0F;
      if (mapWidth <= viewWidth) {
         this.POOL = mapWidth / 2.0F;
      } else {
         viewWidth /= 2.0F;
         this.POOL = MathUtils.clamp(x, viewWidth, mapWidth - viewWidth);
      }

      if (mapHeight <= viewHeight) {
         this.owner = mapHeight / 2.0F;
      } else {
         viewWidth = viewHeight / 2.0F;
         this.owner = MathUtils.clamp(y, viewWidth, mapHeight - viewWidth);
      }

      this.skeleton = true;
   }

   @Override
   public final void dispose() {
   }
}
