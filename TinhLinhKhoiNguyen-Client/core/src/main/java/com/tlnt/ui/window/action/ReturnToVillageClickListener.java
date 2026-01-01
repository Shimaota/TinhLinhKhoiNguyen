package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ReturnToVillageClickListener extends ClickListener {
   public com.tlnt.ui.window.action.DeathRespawnPopup deathRespawnPopup;
   public ReturnToVillageClickListener(DeathRespawnPopup popup) {
      super();
      this.deathRespawnPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestDungeon();
   }
}
