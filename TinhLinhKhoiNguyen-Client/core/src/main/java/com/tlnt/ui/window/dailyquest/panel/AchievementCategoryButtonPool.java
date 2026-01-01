package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class AchievementCategoryButtonPool extends Pool<AchievementPanel.ListPanel.AchievementEntryButton> {
   public com.tlnt.ui.window.dailyquest.panel.AchievementPanel.ListPanel listPanel;
   public ButtonGroup<Button> buttonGroup;

   public AchievementCategoryButtonPool(AchievementPanel.ListPanel listPanel) {
      super();
      this.listPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final AchievementPanel.ListPanel.AchievementEntryButton newObject() {
      AchievementPanel.ListPanel.AchievementEntryButton entryButton = this.listPanel.new AchievementEntryButton(this.listPanel.renderContext);
      this.buttonGroup.add(entryButton);
      return entryButton;
   }
}
