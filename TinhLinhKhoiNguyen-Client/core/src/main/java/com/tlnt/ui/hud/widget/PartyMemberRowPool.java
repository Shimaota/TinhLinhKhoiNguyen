package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.utils.Pool;

public final class PartyMemberRowPool extends Pool<QuestPartyPanel.PartyMemberListPanel.PartyMemberRow> {
   public com.tlnt.ui.hud.widget.QuestPartyPanel.PartyMemberListPanel partyMemberListPanel;
   public PartyMemberRowPool(QuestPartyPanel.PartyMemberListPanel partyMemberListPanel) {
      super();
      this.partyMemberListPanel = partyMemberListPanel;
   }

   @Override
   public QuestPartyPanel.PartyMemberListPanel.PartyMemberRow newObject() {
      return (this.partyMemberListPanel).new PartyMemberRow(this.partyMemberListPanel.renderContext);
   }
}
