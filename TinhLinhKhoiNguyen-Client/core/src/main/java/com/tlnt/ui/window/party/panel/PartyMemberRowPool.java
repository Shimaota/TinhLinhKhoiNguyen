package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;

public final class PartyMemberRowPool extends Pool<PartyMembersPanel.MemberRow> {
   public com.tlnt.ui.window.party.panel.PartyMembersPanel panel;
   public PartyMemberRowPool(PartyMembersPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public PartyMembersPanel.MemberRow newObject() {
      return (this.panel).new MemberRow(this.panel.renderContext);
   }
}
