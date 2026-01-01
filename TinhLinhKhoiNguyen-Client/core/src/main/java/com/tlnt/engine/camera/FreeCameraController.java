package com.tlnt.engine.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;

public final class FreeCameraController extends GameObject {
   public OrthographicCamera POOL2;

   public FreeCameraController(LoginScreen screen) {
      super(screen, EntityType.CAMERA);
      this.POOL2 = screen.getWorldCamera();
   }

   @Override
   public final void update(float delta) {
      if (this.getScreen().isEditorMode()) {
         float horizontal = InputHandler.getInstance().getAxis("Horizontal");
         InputHandler input = InputHandler.getInstance();
         Vector2 move = new Vector2(horizontal, input.getAxis("Vertical")).scl(0.05F);
         this.POOL2.position.add(move.x, move.y, 0.0F);
      }

      this.setPosition(this.POOL2.position.x, this.POOL2.position.y);
   }

   @Override
   public final void setWorldPosition(Vector2 position) {
      this.POOL2.position.set(position, 0.0F);
      this.POOL2.update();
   }

   @Override
   public final boolean containsPoint(Vector2 point) {
      OrthographicCamera camera = this.POOL2;
      float left = this.POOL2.position.x - camera.viewportWidth * 0.5F * camera.zoom;
      float right = camera.position.x + camera.viewportWidth * 0.5F * camera.zoom;
      float bottom = camera.position.y - camera.viewportHeight * 0.5F * camera.zoom;
      float top = camera.position.y + camera.viewportHeight * 0.5F * camera.zoom;
      if (point.x >= left && point.x <= right && point.y >= bottom && point.y <= top) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void dispose() {
   }

}
