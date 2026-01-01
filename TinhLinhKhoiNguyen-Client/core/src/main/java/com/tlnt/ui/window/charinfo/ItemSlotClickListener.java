package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.UnequipItemAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ItemSlotClickListener extends ClickListener {
   public com.tlnt.ui.hud.itemslot.ItemSlotButton slotButton;
   public com.tlnt.ui.window.charinfo.ItemCountSlotButtonPool slotPool;
   public ItemSlotClickListener(ItemCountSlotButtonPool pool, ItemSlotButton button) {
      super();
      this.slotPool = pool;
      this.slotButton = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.slotButton.getItem() != null) {
         ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
         ItemSlotButton anchor = this.slotButton;
         Item item = this.slotButton.getItem();
         UnequipItemAction defaultAction = ItemActionRegistry.UNEQUIP_ACTION;
         ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
         actions[0] = ItemActionRegistry.UNEQUIP_ACTION;
         popup.showItemWithActions(anchor, item, defaultAction, actions);
      }
   }
}
