package com.tlnt.engine.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.tlnt.core.GameMain;
import com.tlnt.engine.render.RenderContext;

public final class InputHandler extends InputAdapter {
   public static InputHandler instance;
   public RenderContext renderContext;
   public long lastPointerFrameId = -1L;
   public final Vector3 tmpUnprojectVec = new Vector3();
   public final Vector2 pointerWorldPos = new Vector2();
   public final Vector2 moveDirection = new Vector2();
   public boolean upPressed;
   public boolean downPressed;
   public boolean leftPressed;
   public boolean rightPressed;
   public float horizontalAxis = 0.0F;
   public float verticalAxis = 0.0F;
   public float axisLerpSpeed = 5.0F;
   public final Vector3 touchTmpVec = new Vector3();

   public static InputHandler getInstance() {
      if (instance == null) {
         instance = new InputHandler();
      }

      return instance;
   }

   public static boolean isKeyPressed(int keycode) {
      return Gdx.input.isKeyPressed(keycode);
   }

   public static boolean isKeyJustPressed(int keycode) {
      return Gdx.input.isKeyJustPressed(keycode);
   }

   public final InputHandler setRenderContext(RenderContext renderContext) {
      this.renderContext = renderContext;
      return this;
   }

   public final Vector2 getPointerWorldPos() {
      long frameId;
      if ((frameId = Gdx.graphics.getFrameId()) != this.lastPointerFrameId) {
         this.tmpUnprojectVec.set((float)Gdx.input.getX(), (float)Gdx.input.getY(), 0.0F);
         if (this.renderContext != null) {
            this.renderContext.screenToWorld(this.tmpUnprojectVec);
         }

         this.pointerWorldPos.set(this.tmpUnprojectVec.x, this.tmpUnprojectVec.y);
         this.lastPointerFrameId = frameId;
      }

      return this.pointerWorldPos;
   }

   public final float getAxis(String axisName) {
      float value = 0.0F;
      int axisIndex = -1;
      switch (axisName.hashCode()) {
         case -1919497322:
            if (axisName.equals("Vertical")) {
               axisIndex = 1;
            }
            break;
         case -913872828:
            if (axisName.equals("Horizontal")) {
               axisIndex = 0;
            }
      }

      switch (axisIndex) {
         case 0:
            if (this.leftPressed && this.rightPressed) {
               value = 0.0F;
            } else if (this.rightPressed) {
               value = 1.0F;
            } else if (this.leftPressed) {
               value = -1.0F;
            }
            break;
         case 1:
            if (this.upPressed && this.downPressed) {
               value = 0.0F;
            } else if (this.upPressed) {
               value = 1.0F;
            } else if (this.downPressed) {
               value = -1.0F;
            }
      }

      float delta = Gdx.graphics.getDeltaTime();
      if (axisName.equals("Horizontal")) {
         this.horizontalAxis = lerpAxis(this.horizontalAxis, value, this.axisLerpSpeed * delta);
         return this.horizontalAxis;
      } else if (axisName.equals("Vertical")) {
         this.verticalAxis = lerpAxis(this.verticalAxis, value, this.axisLerpSpeed * delta);
         return this.verticalAxis;
      } else {
         return 0.0F;
      }
   }

   public static float lerpAxis(float current, float target, float step) {
      if (current < target) {
         return Math.min(current + step, target);
      } else {
         return current > target ? Math.max(current - step, target) : target;
      }
   }

   @Override
   public final boolean keyDown(int keycode) {
      switch (keycode) {
         case 19:
         case 51:
            this.upPressed = true;
            break;
         case 20:
         case 47:
            this.downPressed = true;
            break;
         case 21:
         case 29:
            this.leftPressed = true;
            break;
         case 22:
         case 32:
            this.rightPressed = true;
      }

      return super.keyDown(keycode);
   }

   @Override
   public final boolean keyUp(int keycode) {
      switch (keycode) {
         case 19:
         case 51:
            this.upPressed = false;
            break;
         case 20:
         case 47:
            this.downPressed = false;
            break;
         case 21:
         case 29:
            this.leftPressed = false;
            break;
         case 22:
         case 32:
            this.rightPressed = false;
      }

      return super.keyUp(keycode);
   }

   @Override
   public final boolean touchDown(int screenX, int screenY, int pointer, int button) {
      if (button == 0) {
         this.touchTmpVec.set((float)screenX, (float)screenY, 0.0F);
         if (GameMain.instance.getScreen().equals(GameMain.instance.getLoginScreen())) {
            GameMain.instance.getLoginScreen().getRenderContext().unprojectWorld(this.touchTmpVec);
            return super.touchDown(screenX, screenY, pointer, button);
         }
      }

      return super.touchDown(screenX, screenY, pointer, button);
   }

   public final void setMoveDirection(float x, float y) {
      this.moveDirection.set(x, y).nor();
   }

   public final Vector2 getMoveDirection() {
      return this.moveDirection;
   }

}
