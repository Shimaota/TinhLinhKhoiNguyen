package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ForgeBrewRightSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeBrewPanel.BrewControlPanel brewControlPanel;
   public ForgeBrewRightSlotClickListener(ForgeBrewPanel.BrewControlPanel panel) {
      super();
      this.brewControlPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.brewControlPanel.ingredientSlot3;
      Item item = this.brewControlPanel.ingredientSlot3.getItem();
      OpenItemTooltipAction defaultAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
