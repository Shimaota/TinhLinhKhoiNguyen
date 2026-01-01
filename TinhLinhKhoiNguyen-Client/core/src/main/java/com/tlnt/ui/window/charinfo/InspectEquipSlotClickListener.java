package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.EquipItemAction2;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class InspectEquipSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.charinfo.EquipSlotButtonPool inspectButtonPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton equipButton;
   public InspectEquipSlotClickListener(EquipSlotButtonPool pool, ItemSlotButton button) {
      super();
      this.inspectButtonPool = pool;
      this.equipButton = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.equipButton.getItem() != null) {
         ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
         ItemSlotButton anchor = this.equipButton;
         Item item = this.equipButton.getItem();
         EquipItemAction2 defaultAction = ItemActionRegistry.USE_ITEM_ACTION;
         ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
         actions[0] = ItemActionRegistry.USE_ITEM_ACTION;
         actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
         popup.showItemWithActions(anchor, item, defaultAction, actions);
      }
   }
}
