package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class TitleItemClickListener extends ClickListener {
   public com.tlnt.ui.window.wardrobe.panel.TitleItemPool titleItemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlot;
   public TitleItemClickListener(TitleItemPool pool, ItemSlotButton button) {
      super();
      this.titleItemPool = pool;
      this.itemSlot = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.itemSlot;
      Item item = this.itemSlot.getItem();
      OpenItemTooltipAction defaultAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
