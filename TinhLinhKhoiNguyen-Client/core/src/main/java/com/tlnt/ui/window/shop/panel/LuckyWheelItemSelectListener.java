package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class LuckyWheelItemSelectListener extends ClickListener {
   public com.tlnt.ui.window.shop.panel.LuckyWheelPanel luckyWheelPanel;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemButton;
   public LuckyWheelItemSelectListener(LuckyWheelPanel panel, ItemSlotButton slotButton) {
      super();
      this.luckyWheelPanel = panel;
      this.itemButton = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton source = this.itemButton;
      Item item = this.itemButton.getItem();
      OpenItemTooltipAction closeAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(source, item, closeAction, actions);
   }
}
