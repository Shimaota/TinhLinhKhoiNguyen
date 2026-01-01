package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.ItemActionMenuPopup;

public final class FriendContextMenuListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.FriendListPanel.FriendRow friendRow;
   public com.tlnt.ui.window.party.panel.FriendListPanel friendListPanel;
   public FriendContextMenuListener(FriendListPanel.FriendRow friendRow, FriendListPanel friendListPanel) {
      super();
      this.friendRow = friendRow;
      this.friendListPanel = friendListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Vector2 stagePos = this.friendRow.localToStageCoordinates(new Vector2(x, y));
      ItemActionMenuPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemActionMenuPopup.class);
      float stageX = stagePos.x;
      float stageY = stagePos.y;
      ItemActionMenuPopup.ActionType[] actions = new ItemActionMenuPopup.ActionType[3];
      actions[0] = ItemActionMenuPopup.ActionType.ENCHANT_ITEM.withItemId(this.friendRow.friendData.playerId);
      actions[1] = ItemActionMenuPopup.ActionType.USE_ITEM.withItemId(this.friendRow.friendData.playerId);
      actions[2] = ItemActionMenuPopup.ActionType.STORAGE_DEPOSIT.withItemId(this.friendRow.friendData.index);
      popup.showWithActions(stageX, stageY, 10, actions);
   }
}
