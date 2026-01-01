package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ForgeElementConfirmListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeElementPanel.RecipePanel recipePanel;
   public ForgeElementConfirmListener(ForgeElementPanel.RecipePanel panel) {
      super();
      this.recipePanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendPvpAccept(1);
   }
}
