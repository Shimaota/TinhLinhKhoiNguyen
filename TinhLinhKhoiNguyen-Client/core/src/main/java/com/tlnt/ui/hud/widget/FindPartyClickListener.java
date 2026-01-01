package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class FindPartyClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.QuestPartyPanel.PartyMemberListPanel partyMemberListPanel;
   public FindPartyClickListener(QuestPartyPanel.PartyMemberListPanel partyMemberListPanel) {
      super();
      this.partyMemberListPanel = partyMemberListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendOpenStorage();
   }
}
