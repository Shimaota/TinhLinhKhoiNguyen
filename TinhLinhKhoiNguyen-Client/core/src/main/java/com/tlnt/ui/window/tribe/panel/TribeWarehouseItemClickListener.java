package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.UseItemAction3;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class TribeWarehouseItemClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeWarehouseItemPool itemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlotButton;
   public TribeWarehouseItemClickListener(TribeWarehouseItemPool itemPool, ItemSlotButton itemSlotButton) {
      super();
      this.itemPool = itemPool;
      this.itemSlotButton = itemSlotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slot = this.itemSlotButton;
      Item item = this.itemSlotButton.getItem();
      UseItemAction3 primaryAction = ItemActionRegistry.USE_ACTION_3;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[3];
      actions[0] = ItemActionRegistry.USE_ACTION_3;
      actions[1] = ItemActionRegistry.EQUIP_THIRD_SLOT_ACTION;
      actions[2] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slot, item, primaryAction, actions);
   }
}
