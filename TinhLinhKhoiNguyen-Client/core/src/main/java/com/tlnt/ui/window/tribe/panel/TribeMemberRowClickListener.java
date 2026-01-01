package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.social.TribeMember;
import com.tlnt.ui.window.action.ItemActionMenuPopup;
import com.tlnt.world.entity.player.PlayerEntity;

public final class TribeMemberRowClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeMemberRow memberRow;
   public com.tlnt.ui.window.tribe.panel.TribeMemberRow.MemberButton tribeMemberListPanel;
   public TribeMemberRowClickListener(TribeMemberRow.MemberButton memberButton, TribeMemberRow memberRow) {
      super();
      this.tribeMemberListPanel = memberButton;
      this.memberRow = memberRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Vector2 stagePos = this.tribeMemberListPanel.localToStageCoordinates(new Vector2(x, y));
      ItemActionMenuPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemActionMenuPopup.class);
      float stageX = stagePos.x;
      float stageY = stagePos.y;
      ItemActionMenuPopup.ActionType[] actions = new ItemActionMenuPopup.ActionType[6];
      actions[0] = ItemActionMenuPopup.ActionType.ENCHANT_ITEM.withItemId(this.tribeMemberListPanel.memberData.playerId);
      actions[1] = ItemActionMenuPopup.ActionType.FORGE_UPGRADE.withItemId(this.tribeMemberListPanel.memberData.playerId);
      actions[2] = GameState.tribeData.myTribe.isLeader(PlayerEntity.getInstance().getId())
            && this.tribeMemberListPanel.memberData.role == TribeMember.Role.MEMBER
         ? ItemActionMenuPopup.ActionType.SHOP_BUY.withItemId(this.tribeMemberListPanel.memberData.playerId)
         : null;
      actions[3] = GameState.tribeData.myTribe.isLeader(PlayerEntity.getInstance().getId())
            && this.tribeMemberListPanel.memberData.role == TribeMember.Role.DEPUTY
         ? ItemActionMenuPopup.ActionType.TRADE_ITEM.withItemId(this.tribeMemberListPanel.memberData.playerId)
         : null;
      actions[4] = GameState.tribeData.myTribe.isLeader(PlayerEntity.getInstance().getId())
         ? ItemActionMenuPopup.ActionType.SHOP_ACTION.withItemId(this.tribeMemberListPanel.memberData.playerId)
         : null;
      actions[5] = GameState.tribeData.myTribe.isLeader(PlayerEntity.getInstance().getId())
         ? ItemActionMenuPopup.ActionType.TRIBE_DONATE.withItemId(this.tribeMemberListPanel.memberData.playerId)
         : null;
      popup.showWithActions(stageX, stageY, 10, actions);
   }
}
