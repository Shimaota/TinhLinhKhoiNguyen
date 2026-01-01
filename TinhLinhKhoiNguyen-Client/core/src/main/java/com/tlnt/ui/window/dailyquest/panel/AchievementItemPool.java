package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class AchievementItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.dailyquest.panel.AchievementPanel.DetailPanel parentPanel;
   public ButtonGroup<Button> buttonGroup;

   public AchievementItemPool(AchievementPanel.DetailPanel detailPanel) {
      super();
      this.parentPanel = detailPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.parentPanel.renderContext)).addListener(new AchievementRewardItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
