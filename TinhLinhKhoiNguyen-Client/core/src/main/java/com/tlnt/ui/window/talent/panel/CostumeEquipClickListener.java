package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.AutoPickupFilterActionConfig;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class CostumeEquipClickListener extends ClickListener {
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlotButton;
   public com.tlnt.ui.window.talent.panel.TitleItemPool titleItemPool;
   public CostumeEquipClickListener(TitleItemPool pool, ItemSlotButton slotButton) {
      super();
      this.titleItemPool = pool;
      this.itemSlotButton = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.itemSlotButton;
      Item item = this.itemSlotButton.getItem();
      AutoPickupFilterActionConfig defaultAction = ItemActionRegistry.SET_AUTO_PICKUP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[3];
      actions[0] = ItemActionRegistry.SET_AUTO_PICKUP_ACTION;
      actions[1] = ItemActionRegistry.SEND_TO_STORAGE_ACTION;
      actions[2] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
