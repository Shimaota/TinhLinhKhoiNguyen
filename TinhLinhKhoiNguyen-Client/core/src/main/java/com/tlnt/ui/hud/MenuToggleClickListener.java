package com.tlnt.ui.hud;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.engine.WorldConfig;
import com.tlnt.ui.hud.widget.MenuButtonPanel;

public final class MenuToggleClickListener extends ClickListener {
   public com.tlnt.ui.hud.GameHud gameHud;
   public MenuToggleClickListener(GameHud gameHud) {
      super();
      this.gameHud = gameHud;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      MenuButtonPanel panel = this.gameHud.menuButtonPanel;
      if (this.gameHud.menuButtonPanel.getActions().isEmpty()) {
         if (panel.isVisible()) {
            panel.addAction(
               Actions.sequence(
                  Actions.moveTo((float)WorldConfig.VIEWPORT_WIDTH, panel.getY(), 0.5F, Interpolation.swingIn),
                  Actions.visible(false)
               )
            );
            return;
         }

         panel.setVisible(true);
         float rightInset = Gdx.app.getType() != Application.ApplicationType.iOS ? 7.0F : 109.0F;
         panel.addAction(
            Actions.sequence(
               Actions.moveTo(
                  (float)WorldConfig.VIEWPORT_WIDTH - panel.getWidth() - rightInset,
                  panel.getY(),
                  0.5F,
                  Interpolation.swingOut
               )
            )
         );
      }
   }
}
