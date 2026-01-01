package com.tlnt.ui.window.party;

import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.party.tab.PartyFriendsTab;
import com.tlnt.ui.window.party.tab.PartyJoinTab;
import com.tlnt.ui.window.party.tab.PartyMembersTab;
import com.tlnt.ui.window.party.tab.PartyNearbyTab;
import com.tlnt.ui.window.party.tab.PartyRequestsTab;

public final class PartyWindow extends TabbedWindow {

   public PartyWindow(RenderContext renderContext) {
      super(renderContext, WindowType.TEAM_POPUP);
      this.addTabWithCallback(
         "Tổ đội", "icon/263", new PartyJoinTab(super.renderContext), () -> {
            if (GameState.currentParty == null) {
               return true;
            } else {
               return false;
            }
         }
      );
      String tabTitle = "Tổ đội";
      this.addTabWithCallback(tabTitle, "icon/263", new PartyMembersTab(super.renderContext), () -> {
         if (GameState.currentParty != null) {
            return true;
         } else {
            return false;
         }
      });
      this.addTabWithCallback("Lân cận", "icon/279", new PartyNearbyTab(super.renderContext), () -> {
         if (GameState.currentParty != null) {
            return true;
         } else {
            return false;
         }
      });
      this.addTabFull("Chờ duyệt", "icon/265", new PartyRequestsTab(super.renderContext), () -> {
         if (GameState.currentParty != null) {
            return true;
         } else {
            return false;
         }
      }, new PartyRequestsBadgeChecker(this));
      this.addTabFull(
         "Bạn bè", "icon/264", new PartyFriendsTab(super.renderContext), null, new FriendsBadgeChecker(this)
      );
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

}
