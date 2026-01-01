package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class GiftcodeRewardItemClickListener extends ClickListener {
   public com.tlnt.ui.hud.itemslot.ItemSlotButton rewardItem;
   public GiftcodeEventPanel$RewardItemPool itemPool;
   public GiftcodeRewardItemClickListener(GiftcodeEventPanel$RewardItemPool pool, ItemSlotButton slotButton) {
      super();
      this.itemPool = pool;
      this.rewardItem = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.rewardItem;
      Item item = this.rewardItem.getItem();
      OpenItemTooltipAction defaultAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
