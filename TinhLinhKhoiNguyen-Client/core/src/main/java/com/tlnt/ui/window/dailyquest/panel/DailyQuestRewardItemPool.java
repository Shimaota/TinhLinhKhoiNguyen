package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class DailyQuestRewardItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.DailyQuestListPanel parentPanel;
   public ButtonGroup<ItemSlotButton> buttonGroup;

   public DailyQuestRewardItemPool(DailyQuestPanel.DailyQuestListPanel listPanel) {
      super();
      this.parentPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.parentPanel.renderContext)).addListener(new ItemRewardIconClickListener(this, slotButton));
      this.parentPanel.dailyQuestPanel.rewardIconButtonGroup.add(slotButton);
      return slotButton;
   }
}
