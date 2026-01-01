package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.RemoveInlayAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ForgeSoulSlot6SelectListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeSoulPanel.SlotPanel slotPanel;
   public ForgeSoulSlot6SelectListener(ForgeSoulPanel.SlotPanel panel) {
      super();
      this.slotPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.slotPanel.lowerRightSlot;
      Item item = this.slotPanel.lowerRightSlot.getItem();
      RemoveInlayAction defaultAction = ItemActionRegistry.DETACH_INLAY_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.DETACH_INLAY_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
