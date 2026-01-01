package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class EquipSkillTypeClickListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.TalentPanel.SkillDetailPanel skillDetailPanel;
   public EquipSkillTypeClickListener(TalentPanel.SkillDetailPanel detailPanel) {
      super();
      this.skillDetailPanel = detailPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.skillDetailPanel.talentPanel.selectedSkillSlot != null) {
         ServerPacketSender.getInstance().sendDropItem(this.skillDetailPanel.talentPanel.selectedSkillSlot.skill.id);
      }
   }
}
