package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.ItemActionMenuPopup;

public final class PartyMemberItemActionListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyMembersPanel.MemberRow memberRow;
   public com.tlnt.ui.window.party.panel.PartyMembersPanel panel;
   public PartyMemberItemActionListener(PartyMembersPanel.MemberRow memberRow, PartyMembersPanel panel) {
      super();
      this.memberRow = memberRow;
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Vector2 stagePos = this.memberRow.localToStageCoordinates(new Vector2(x, y));
      ItemActionMenuPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemActionMenuPopup.class);
      float stageX = stagePos.x;
      float stageY = stagePos.y;
      ItemActionMenuPopup.ActionType[] actions = new ItemActionMenuPopup.ActionType[4];
      actions[0] = ItemActionMenuPopup.ActionType.ENCHANT_ITEM.withItemId(this.memberRow.memberData.playerId);
      actions[1] = ItemActionMenuPopup.ActionType.FORGE_UPGRADE.withItemId(this.memberRow.memberData.playerId);
      actions[2] = ItemActionMenuPopup.ActionType.SPLIT_ITEM.withItemId(this.memberRow.memberData.playerId);
      actions[3] = ItemActionMenuPopup.ActionType.DESTROY_ITEM.withItemId(this.memberRow.memberData.playerId);
      popup.showWithActions(stageX, stageY, 10, actions);
   }
}
