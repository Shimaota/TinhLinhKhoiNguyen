package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class MainQuestRewardIconPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.dailyquest.panel.MainQuestPanel.RewardItemsPanel rewardPanel;
   public ButtonGroup<Button> rewardButtonGroup;

   public MainQuestRewardIconPool(MainQuestPanel.RewardItemsPanel rewardPanel) {
      super();
      this.rewardPanel = rewardPanel;
      this.rewardButtonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.rewardPanel.renderContext)).addListener(new QuestRewardItemClickListener(this, slotButton));
      this.rewardButtonGroup.add(slotButton);
      return slotButton;
   }
}
