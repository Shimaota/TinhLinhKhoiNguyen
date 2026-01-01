package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.social.TribeMember;
import com.tlnt.ui.window.action.ItemActionMenuPopup;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PlayerPortraitClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.PlayerPortrait portrait;
   public PlayerPortraitClickListener(PlayerPortrait portrait) {
      super();
      this.portrait = portrait;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemActionMenuPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemActionMenuPopup.class);
      float popupX = this.portrait.getX();
      float popupY = this.portrait.getY();
      ItemActionMenuPopup.ActionType[] actions = new ItemActionMenuPopup.ActionType[6];
      actions[0] = ItemActionMenuPopup.ActionType.ENCHANT_ITEM.withItemId(this.portrait.playerEntity.getId());
      actions[1] = GameState.tribeData.myTribe != null
            && GameState.tribeData.myTribe.getMemberRole(PlayerEntity.getInstance().getId()) != TribeMember.Role.MEMBER
         ? ItemActionMenuPopup.ActionType.SHOP_SELL.withItemId(this.portrait.playerEntity.getId())
         : null;
      actions[2] = GameState.currentParty != null ? ItemActionMenuPopup.ActionType.USE_ITEM.withItemId(this.portrait.playerEntity.getId()) : null;
      actions[3] = ItemActionMenuPopup.ActionType.FORGE_UPGRADE.withItemId(this.portrait.playerEntity.getId());
      actions[4] = ItemActionMenuPopup.ActionType.SKILL_UPGRADE.withItemId(this.portrait.playerEntity.getId());
      actions[5] = ItemActionMenuPopup.ActionType.UNLOCK_ITEM.withItemId(this.portrait.playerEntity.getId());
      popup.showWithActions(popupX, popupY, 10, actions);
   }
}
