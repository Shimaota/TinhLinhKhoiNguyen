package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class TalentTypeButtonPool extends Pool<TalentPanel.TalentTypeButton> {
   public final com.tlnt.ui.window.talent.panel.TalentPanel.SkillListPanel skillListPanel;
   public ButtonGroup<Button> buttonGroup;

   public TalentTypeButtonPool(TalentPanel.SkillListPanel listPanel) {
      this.skillListPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final TalentPanel.TalentTypeButton newObject() {
      TalentPanel.TalentTypeButton typeButton;
      (typeButton = new TalentPanel.TalentTypeButton(this.skillListPanel.renderContext))
         .addListener(new TalentTypeButtonClickListener(this, typeButton));
      this.buttonGroup.add(typeButton);
      return typeButton;
   }
}
