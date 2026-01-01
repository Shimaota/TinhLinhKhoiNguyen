package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.DonateToTribeActionHandler;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ForgeSoulSlot1SelectListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeSoulPanel.SlotPanel slotPanel;
   public ForgeSoulSlot1SelectListener(ForgeSoulPanel.SlotPanel panel) {
      super();
      this.slotPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.slotPanel.topLeftSlot;
      Item item = this.slotPanel.topLeftSlot.getItem();
      DonateToTribeActionHandler defaultAction = ItemActionRegistry.DONATE_TO_TRIBE_HANDLER;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.DONATE_TO_TRIBE_HANDLER;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
