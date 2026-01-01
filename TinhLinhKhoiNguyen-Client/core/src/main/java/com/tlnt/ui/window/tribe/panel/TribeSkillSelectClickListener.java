package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class TribeSkillSelectClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeSkillPanel tribeSkillPanel;
   public com.tlnt.ui.window.tribe.panel.TribeSkillPanel.SkillIconButton skillIconButton;
   public TribeSkillSelectClickListener(TribeSkillPanel.SkillIconButton skillIconButton, TribeSkillPanel tribeSkillPanel) {
      super();
      this.skillIconButton = skillIconButton;
      this.tribeSkillPanel = tribeSkillPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.skillIconButton.tribeSkillPanel.skillContent.setSelectedSkill(this.skillIconButton.skillData);
   }
}
