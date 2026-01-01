package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SkillButtonClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.SkillButton skillButton;
   public SkillButtonClickListener(SkillButton skillButton) {
      super();
      this.skillButton = skillButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.skillButton.activateSkill();
   }
}
