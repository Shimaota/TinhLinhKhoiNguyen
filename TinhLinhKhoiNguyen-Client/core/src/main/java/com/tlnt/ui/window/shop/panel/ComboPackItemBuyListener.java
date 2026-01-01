package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ComboPackItemBuyListener extends ClickListener {
   public com.tlnt.ui.window.shop.panel.ComboPackItemPool itemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemButton;
   public ComboPackItemBuyListener(ComboPackItemPool pool, ItemSlotButton slotButton) {
      super();
      this.itemPool = pool;
      this.itemButton = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      if (popup.shouldBlockShopClick()) {
         event.stop();
         event.handle();
         return;
      }
      ItemSlotButton source = this.itemButton;
      Item item = this.itemButton.getItem();
      OpenItemTooltipAction closeAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(source, item, closeAction, actions);
      event.stop();
      event.handle();
   }
}
