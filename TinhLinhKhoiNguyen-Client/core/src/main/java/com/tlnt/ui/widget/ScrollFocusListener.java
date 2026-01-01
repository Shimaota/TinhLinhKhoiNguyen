package com.tlnt.ui.widget;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;

public final class ScrollFocusListener extends InputListener {
   public com.tlnt.ui.widget.StyledScrollPane scrollPane;
   public ScrollFocusListener(StyledScrollPane scrollPane) {
      super();
      this.scrollPane = scrollPane;
   }

   @Override
   public final void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
      this.scrollPane.getStage().setScrollFocus(this.scrollPane);
      super.enter(event, x, y, pointer, fromActor);
   }

   @Override
   public final boolean mouseMoved(InputEvent event, float x, float y) {
      this.scrollPane.getStage().setScrollFocus(this.scrollPane);
      return super.mouseMoved(event, x, y);
   }

   @Override
   public final void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
      Stage stage;
      if ((stage = this.scrollPane.getStage()) != null) {
         stage.setScrollFocus(null);
      }

      super.exit(event, x, y, pointer, toActor);
   }
}
