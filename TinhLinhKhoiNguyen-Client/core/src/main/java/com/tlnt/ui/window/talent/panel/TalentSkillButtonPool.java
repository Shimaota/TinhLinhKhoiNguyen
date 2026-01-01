package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class TalentSkillButtonPool extends Pool<TalentPanel.SkillButton> {
   public final com.tlnt.ui.window.talent.panel.TalentPanel.SkillListPanel skillListPanel;
   public ButtonGroup<Button> buttonGroup;

   public TalentSkillButtonPool(TalentPanel.SkillListPanel listPanel) {
      this.skillListPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final TalentPanel.SkillButton newObject() {
      TalentPanel.SkillButton skillButton;
      (skillButton = this.skillListPanel.talentPanel.new SkillButton(this.skillListPanel.renderContext))
         .addListener(new TalentSkillButtonClickListener(this, skillButton));
      this.buttonGroup.add(skillButton);
      return skillButton;
   }
}
