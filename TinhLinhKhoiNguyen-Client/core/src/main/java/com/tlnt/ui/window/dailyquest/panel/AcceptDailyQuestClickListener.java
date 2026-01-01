package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class AcceptDailyQuestClickListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.DailyQuestListPanel questDetailPanel;
   public AcceptDailyQuestClickListener(DailyQuestPanel.DailyQuestListPanel listPanel) {
      super();
      this.questDetailPanel = listPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendPetStay();
   }
}
