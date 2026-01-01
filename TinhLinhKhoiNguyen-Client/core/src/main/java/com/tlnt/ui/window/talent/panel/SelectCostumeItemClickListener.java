package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.DropItemAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class SelectCostumeItemClickListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.CostumeItemPool costumeItemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlotButton;
   public SelectCostumeItemClickListener(CostumeItemPool pool, ItemSlotButton slotButton) {
      super();
      this.costumeItemPool = pool;
      this.itemSlotButton = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.itemSlotButton;
      Item item = this.itemSlotButton.getItem();
      DropItemAction defaultAction = ItemActionRegistry.SELECT_CHAR_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[3];
      actions[0] = ItemActionRegistry.SELECT_CHAR_ACTION;
      actions[1] = ItemActionRegistry.REMOVE_SKILL_ACTION;
      actions[2] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
