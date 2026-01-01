package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.ui.window.GamePanel;

public final class TabVisibilityToggleListener extends ClickListener {
   public com.tlnt.ui.window.action.TabbedWindow tabbedWindow;
   public com.tlnt.ui.window.GamePanel gamePanel;
   public TabVisibilityToggleListener(TabbedWindow window, GamePanel panel) {
      super();
      this.tabbedWindow = window;
      this.gamePanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (!this.gamePanel.isVisible()) {
         for (Actor child : this.tabbedWindow.contentGroup.getChildren()) {
            child.setVisible(false);
         }

         this.gamePanel.setVisible(true);
      }
   }
}
