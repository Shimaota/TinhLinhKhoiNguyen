package com.tlnt.ui.window.action;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.WindowType;

public abstract class Popup extends Dialog {
   public float originX;
   public float originY;
   public int positionAlignment;

   public Popup(RenderContext renderContext, WindowType windowType, float x, float y, int alignment, float width, float height) {
      super(renderContext, windowType);
      this.setSize(width, height);
      this.setPosition(this.originX = x, this.originY = y, this.positionAlignment = alignment);
      this.setVisible(false);
   }

   public boolean isDismissOnOutsideClick() {
      return false;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isDismissOnOutsideClick() && Gdx.input.isButtonJustPressed(0)) {
         Vector2 stagePos;
         float stageX = (stagePos = this.getStage().screenToStageCoordinates(new Vector2((float)Gdx.input.getX(), (float)Gdx.input.getY()))).x;
         float stageY = stagePos.y;
         Actor hitActor;
         if ((hitActor = this.hit(stageX - this.getX(), stageY - this.getY(), true)) == null || !hitActor.isDescendantOf(this)) {
            this.hide();
         }
      } else {
         InputHandler.getInstance();
         if (InputHandler.isKeyJustPressed(111)) {
            this.setVisible(false);
         }
      }

      this.drawBackground();
      super.draw(batch, parentAlpha);
   }

   public void drawBackground() {
   }

   @Override
   public void onClose() {
      if (this.getStage() != null) {
         this.getStage().setKeyboardFocus(null);
      }
   }

   public boolean isModal() {
      return true;
   }
}
