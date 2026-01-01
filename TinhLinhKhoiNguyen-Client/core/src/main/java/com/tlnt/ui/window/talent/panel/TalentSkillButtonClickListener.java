package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class TalentSkillButtonClickListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.TalentPanel.SkillButton skillButton;
   public com.tlnt.ui.window.talent.panel.TalentSkillButtonPool skillButtonPool;
   public TalentSkillButtonClickListener(TalentSkillButtonPool pool, TalentPanel.SkillButton button) {
      super();
      this.skillButtonPool = pool;
      this.skillButton = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.skillButtonPool.skillListPanel.talentPanel.showSkillSlotInfo(this.skillButton.skill);
   }
}
