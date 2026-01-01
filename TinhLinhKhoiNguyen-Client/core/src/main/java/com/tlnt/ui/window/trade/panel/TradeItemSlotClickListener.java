package com.tlnt.ui.window.trade.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class TradeItemSlotClickListener extends ClickListener {
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlotButton;
   public com.tlnt.ui.window.trade.panel.TradeOfferSlotPool itemSlotPool;
   public TradeItemSlotClickListener(TradeOfferSlotPool pool, ItemSlotButton slotButton) {
      super();
      this.itemSlotPool = pool;
      this.itemSlotButton = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Object action = this.itemSlotPool.itemGridPanel.isMyPanel && !GameState.tradeSession.myOfferLocked
         ? ItemActionRegistry.REMOVE_TRADE_ITEM_ACTION
         : ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton source = this.itemSlotButton;
      Item item = this.itemSlotButton.getItem();
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = (ItemInfoPopup.ActionConfig)action;
      popup.showItemWithActions(source, item, (ItemInfoPopup.ActionConfig)action, actions);
   }
}
