package com.tlnt.ui.window.skillslot;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class SkillSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.skillslot.SkillSlotPopup skillSlotPopup;
   public com.tlnt.ui.window.skillslot.SkillSlotPopup.SkillSlotButton skillSlotButton;
   public SkillSlotClickListener(SkillSlotPopup.SkillSlotButton slotButton, SkillSlotPopup popup) {
      super();
      this.skillSlotButton = slotButton;
      this.skillSlotPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.skillSlotButton.outerPopup.selectedSkill != null) {
         ServerPacketSender.getInstance().sendEquipItem(this.skillSlotButton.outerPopup.selectedSkill.skill.id, this.skillSlotButton.slotIndex);
      }

      this.skillSlotButton.outerPopup.hide();
   }
}
