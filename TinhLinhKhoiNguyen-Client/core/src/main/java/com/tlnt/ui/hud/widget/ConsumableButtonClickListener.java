package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ConsumableButtonClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.HudSkillButton3 hudSkillButton;
   public ConsumableButtonClickListener(HudSkillButton3 hudSkillButton) {
      super();
      this.hudSkillButton = hudSkillButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.hudSkillButton.activateSkill();
   }
}
