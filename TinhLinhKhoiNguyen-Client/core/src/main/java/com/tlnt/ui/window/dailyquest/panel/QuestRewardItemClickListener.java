package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.OpenItemTooltipAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class QuestRewardItemClickListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.MainQuestRewardIconPool itemsPanel;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemButton;
   public QuestRewardItemClickListener(MainQuestRewardIconPool pool, ItemSlotButton slotButton) {
      super();
      this.itemsPanel = pool;
      this.itemButton = slotButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.itemButton;
      Item item = this.itemButton.getItem();
      OpenItemTooltipAction defaultAction = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
