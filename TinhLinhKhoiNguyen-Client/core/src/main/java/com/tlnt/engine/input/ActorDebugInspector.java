package com.tlnt.engine.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.DebugCursorText;

public final class ActorDebugInspector extends InputAdapter {
   public final Stage stage;
   public final Vector2 tmpStageCoords = new Vector2();
   public final Array<Actor> debugActors = new Array<>();
   public Actor hoveredActor;

   public ActorDebugInspector(Stage stage) {
      this.stage = stage;
   }

   @Override
   public final boolean mouseMoved(int screenX, int screenY) {
      return false;
   }

   @Override
   public final boolean touchDragged(int screenX, int screenY, int pointer) {
      return this.mouseMoved(screenX, screenY);
   }

   @Override
   public final boolean keyUp(int keycode) {
      if (keycode == 129 || keycode == 130) {
         ActorDebugInspector inspector = this;

         for (int i = 0; i < inspector.debugActors.size; i++) {
            Actor actor;
            if ((actor = inspector.debugActors.get(i)) != null && actor.getStage() != null) {
               actor.setDebug(false);
            }
         }

         inspector.debugActors.clear();
         this.hoveredActor = null;
         DebugCursorText.text = "";
      }

      return false;
   }

   @Override
   public final boolean touchDown(int screenX, int screenY, int pointer, int button) {
      this.stage.setKeyboardFocus(null);
      return super.touchDown(screenX, screenY, pointer, button);
   }
}
