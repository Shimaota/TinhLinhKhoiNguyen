package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog;

public final class LearnSkillClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeSkillPanel.SkillDetailPane skillDetailPane;
   public LearnSkillClickListener(TribeSkillPanel.SkillDetailPane skillDetailPane) {
      super();
      this.skillDetailPane = skillDetailPane;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.skillDetailPane.selectedSkill != null) {
         int skillId = this.skillDetailPane.selectedSkill.id;
         GiftcodeEventConfirmDialog confirmDialog = GameMain.instance.getLoginScreen().debugScrollPane
            .findDialog(GiftcodeEventConfirmDialog.class);
         confirmDialog.showConfirm(
            "Bạn có chắc muốn đổi sang kỹ năng '" + this.skillDetailPane.selectedSkill.name + "' không?",
            () -> ServerPacketSender.getInstance().sendPetAction(skillId)
         );
         confirmDialog.toFront();
         confirmDialog.show();
      }
   }
}
