package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog;

public final class TribeSkillActionClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeSkillPanel.SkillDetailPane skillDetailPane;
   public TribeSkillActionClickListener(TribeSkillPanel.SkillDetailPane skillDetailPane) {
      super();
      this.skillDetailPane = skillDetailPane;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GiftcodeEventConfirmDialog confirmDialog = GameMain.instance.getLoginScreen().debugScrollPane
         .findDialog(GiftcodeEventConfirmDialog.class);
      confirmDialog.showConfirm("Bạn có chắc muốn mở thêm một kỹ năng Bộ lạc không?", () -> {
         ServerPacketSender.getInstance().sendRequestPet();
      });
      confirmDialog.toFront();
      confirmDialog.show();
   }
}
