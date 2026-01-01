package com.tlnt.engine.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameMain;
import com.tlnt.engine.WorldConfig;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;

public final class CameraPanHandler extends InputAdapter {
   public boolean panning;
   public Vector2 lastTouchPos = new Vector2();

   @Override
   public final boolean touchDown(int screenX, int screenY, int pointer, int button) {
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().POOL2.isAlive) {
         this.lastTouchPos.set((float)screenX, (float)screenY);
         this.panning = true;
         return true;
      } else {
         return super.touchDown(screenX, screenY, pointer, button);
      }
   }

   @Override
   public final boolean touchUp(int screenX, int screenY, int pointer, int button) {
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().POOL2.isAlive) {
         this.panning = false;
         return true;
      } else {
         return super.touchUp(screenX, screenY, pointer, button);
      }
   }

   @Override
   public final boolean touchDragged(int screenX, int screenY, int pointer) {
      if (this.panning && PlayerEntity.getInstance() != null && PlayerEntity.getInstance().POOL2.isAlive) {
         LoginScreen loginScreen = GameMain.instance.getLoginScreen();
         float dx = (float)screenX - this.lastTouchPos.x;
         float dy = (float)screenY - this.lastTouchPos.y;
         this.lastTouchPos.set((float)screenX, (float)screenY);
         OrthographicCamera camera;
         float scaleX = (camera = loginScreen.getWorldCamera()).viewportWidth / (float)WorldConfig.VIEWPORT_WIDTH;
         float scaleY = camera.viewportHeight / (float)WorldConfig.VIEWPORT_HEIGHT;
         dx *= scaleX * camera.zoom;
         float scaledDy;
         dy = -(scaledDy = dy * scaleY * camera.zoom);
         loginScreen.cameraTarget.add(-dx, -dy);
         return true;
      } else {
         return super.touchDragged(screenX, screenY, pointer);
      }
   }
}
