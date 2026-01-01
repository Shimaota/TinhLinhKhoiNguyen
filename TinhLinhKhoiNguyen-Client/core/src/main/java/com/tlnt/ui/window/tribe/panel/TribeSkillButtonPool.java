package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class TribeSkillButtonPool extends Pool<TribeSkillPanel.SkillIconButton> {
   public com.tlnt.ui.window.tribe.panel.TribeSkillPanel.SkillDetailPane skillDetailPane;
   public ButtonGroup<Button> skillButtonGroup;

   public TribeSkillButtonPool(TribeSkillPanel.SkillDetailPane skillDetailPane) {
      super();
      this.skillDetailPane = skillDetailPane;
      this.skillButtonGroup = new ButtonGroup<>();
      // Only one skill can be selected, but deselecting is allowed so another skill slot can be unlocked.
      this.skillButtonGroup.setMaxCheckCount(1);
      this.skillButtonGroup.setMinCheckCount(0);
   }

   @Override
   public final TribeSkillPanel.SkillIconButton newObject() {
      TribeSkillPanel.SkillIconButton button = this.skillDetailPane.tribeSkillPanel.new SkillIconButton(
         this.skillDetailPane.renderContext
      );
      this.skillButtonGroup.add(button);
      return button;
   }
}
