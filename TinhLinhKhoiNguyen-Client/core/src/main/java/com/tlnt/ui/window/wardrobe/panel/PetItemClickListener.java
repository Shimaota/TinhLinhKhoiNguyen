package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.GuildDepositGoldItemAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class PetItemClickListener extends ClickListener {
   public com.tlnt.ui.window.wardrobe.panel.PetItemPool itemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlot;
   public PetItemClickListener(PetItemPool pool, ItemSlotButton button) {
      super();
      this.itemPool = pool;
      this.itemSlot = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.itemSlot;
      Item item = this.itemSlot.getItem();
      GuildDepositGoldItemAction defaultAction = ItemActionRegistry.DEPOSIT_GUILD_GOLD_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[3];
      actions[0] = ItemActionRegistry.DEPOSIT_GUILD_GOLD_ACTION;
      actions[1] = ItemActionRegistry.STORE_ITEM_ACTION;
      actions[2] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
