package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class TalentTypeButtonClickListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.TalentTypeButtonPool talentTypeButtonPool;
   public com.tlnt.ui.window.talent.panel.TalentPanel.TalentTypeButton talentTypeButton;
   public TalentTypeButtonClickListener(TalentTypeButtonPool pool, TalentPanel.TalentTypeButton button) {
      super();
      this.talentTypeButtonPool = pool;
      this.talentTypeButton = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.talentTypeButtonPool.skillListPanel.talentPanel.showSkillInfo(this.talentTypeButton.talentType);
   }
}
