package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.skillslot.SkillSlotPopup;

public final class OpenSkillSlotPopupListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.TalentPanel.SkillDetailPanel skillDetailPanel;
   public OpenSkillSlotPopupListener(TalentPanel.SkillDetailPanel detailPanel) {
      super();
      this.skillDetailPanel = detailPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.skillDetailPanel.talentPanel.selectedSkillSlot != null) {
         GameMain.instance.getLoginScreen().debugScrollPane.findDialog(SkillSlotPopup.class).openWithSkill(this.skillDetailPanel.talentPanel.selectedSkillSlot);
      }
   }
}
